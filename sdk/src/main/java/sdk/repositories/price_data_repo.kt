package sdk.repositories

import sdk.api.StockDataApiProvider
import sdk.api.StockDataPeriods
import sdk.api.getPeriodFromString
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.network.BasicResponseTypes
import sdk.models.*
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import sdk.models.data.assets.Asset
import sdk.models.data.assets.AssetClass
import sdk.models.data.assets.AssetId
import sdk.models.data.assets.Region
import java.time.Duration
import java.time.LocalDateTime

open class PriceDataRepo(
    storageHandler: GenericStorage,
    apiProvider: StockDataApiProvider,
    val sessionProvider: SessionProvider,
    val assetProvider: AssetProvider
) : GenericRepository<Map<AssetId, Map<StockDataPeriods, PriceDataSeries>>, PriceDataIdentifier, StockDataApiProvider>(storageHandler = storageHandler,apiProvider = apiProvider) {

    companion object {
        const val closeKey = "c"
        const val openKey = "o"
        const val lowKey = "l"
        const val highKey = "h"
        const val volumeKey = "v"
        const val dateKey = "d"
    }

    fun toTimeSeriesDataPointList(
        rawData: List<Map<String, Any?>>?,
        period: StockDataPeriods,
        region: Region?,
        assetClass: AssetClass?,
        isIndex: Boolean = false
    ): List<PriceDataPoint> {

        rawData?.let {
            if (it.isEmpty()) {
                return emptyList()
            }
            val date = DateTime.fromSinceEpochMilliSecond((rawData.last()[dateKey] as Double).toLong())

            val periodStart = getPeriodStartDate(
                period,
                date,
                region,
                assetClass
            )
            val firstDateTime = DateTime.fromSinceEpochMilliSecond((rawData.first()[dateKey] as Double).toLong())
            val diffInMinutes = Duration.between(firstDateTime, periodStart).toMinutes()

            val diff = if (period == StockDataPeriods.Price1W) {
                0
            } else {
                diffInMinutes.div(period.periodMinutes).toInt()

            }

            var index = 0

            return rawData.mapNotNull { dataPoint ->
                val date = DateTime.fromSinceEpochMilliSecond((dataPoint[dateKey] as Double).toLong())
                index++
                if (date.isBefore(periodStart) && !isIndex) {
                    null
                } else {
                    PriceDataPoint(
                        index + diff,
                        (dataPoint[dateKey] as Double).toLong(),
                        dataPoint[closeKey] as Double,
                        dataPoint[openKey]?.let { it as? Double }
                            ?: dataPoint[closeKey] as Double,
                        dataPoint[lowKey]?.let { it as? Double } ?: dataPoint[closeKey] as Double,
                        dataPoint[highKey]?.let { it as? Double }
                            ?: dataPoint[closeKey] as Double,
                        (dataPoint[volumeKey]?.let { it as? Int } ?: 1) * 1000
                    )
                }
            }
        } ?: run {
            return emptyList()
        }
    }

    fun getPeriodStartDate(
        period: StockDataPeriods,
        lastDataPoint: LocalDateTime,
        region: Region?,
        assetClass: AssetClass?
    ): LocalDateTime {
        val periodStartDate: LocalDateTime = when (period) {
            StockDataPeriods.Price1D -> sessionProvider.getDayStart(date = lastDataPoint, region = region, assetClass = assetClass)
            StockDataPeriods.Price1W -> lastDataPoint.minus(if (assetClass == AssetClass.Crypto) Duration.ofDays(7) else Duration.ofDays(5))
            StockDataPeriods.Price1M -> lastDataPoint.minusMonths(1).plusDays(2)
            StockDataPeriods.Price3M -> lastDataPoint.minusMonths(3).plusDays(2)
            StockDataPeriods.Price1Y -> lastDataPoint.minusYears(1).plusDays(2)
            StockDataPeriods.Price5Y -> lastDataPoint.minusYears(5).plusDays(2)
            StockDataPeriods.PriceAllTime -> lastDataPoint.minus(Duration.ofDays(7))
        }
        return sessionProvider.getNextTradingDay(date = periodStartDate, assetClass = assetClass, region = region)
    }

    fun adjustInitialPoint(
        cleanData: List<PriceDataPoint>,
        period: StockDataPeriods,
        region: Region?,
        assetClass: AssetClass?
    ): List<PriceDataPoint> {
        if (cleanData.isEmpty()) {
            return cleanData
        } else {
            val periodStartDate = getPeriodStartDate(period, cleanData.last().time, region, assetClass)
            val modifiedData = cleanData.toMutableList()


            while (Duration.between(cleanData.first().time, periodStartDate).toMinutes() > period.periodMinutes) {
                val firstPoint = cleanData.first()
                modifiedData.add(
                    0,
                    PriceDataPoint(
                        firstPoint.index - 1,
                        firstPoint.time.minus(Duration.ofMinutes(period.periodMinutes.toLong())).toEpochMilliSecond(),
                        firstPoint.value,
                        firstPoint.open,
                        firstPoint.low,
                        firstPoint.high,
                        firstPoint.volume
                    )
                )
            }

            return modifiedData
        }
    }

    fun processSinglePriceData(
        asset: Asset,
        period: StockDataPeriods,
        region: Region,
        assetClass: AssetClass,
        data: List<Map<String, Any>>,
        previousClose: Double?
    ): PriceDataSeries? {
        if (data.isEmpty()) {
            return null
        }

        val filteredData = data.filterNot { it["c"] == 0.0 }

        val isIndex = listOf("XU100", "USDTRY", "SGLD").any { it == asset.symbol }

        val cleanData = toTimeSeriesDataPointList(
            rawData = filteredData,
            period = period,
            region = region,
            assetClass = assetClass,
            isIndex = isIndex
        )
        val trimmedData = adjustInitialPoint(
            cleanData = cleanData,
            period = period,
            region = region,
            assetClass = assetClass
        )
        return PriceDataSeries(
            trimmedData,
            asset,
            period,
            if (period == StockDataPeriods.Price1D) previousClose ?: trimmedData.initialValue else trimmedData.initialValue,
            LocalDateTime.now(),
            )
    }

    override suspend fun fetchData(identifier: PriceDataIdentifier?): Map<AssetId, Map<StockDataPeriods, PriceDataSeries>>? {
        identifier ?: return null

        val assets = identifier.assets
        val periods = identifier.periods
        val response = apiProvider.getStockPriceData(
            locale = identifier.region,
            assetClass = identifier.assetClass,
            symbols = assets.map { it.symbol },
            fields = periods
        )

        if (response.responseType != BasicResponseTypes.Success || response.data == null) {
            return null
        }

        val res = mutableMapOf<AssetId, MutableMap<StockDataPeriods, PriceDataSeries>>()
        for (rawData in response.data) {
            val symbol = rawData["symbol"] as String
            val asset = assets.firstOrNull { it.symbol == symbol }

            if (asset != null) {
                for ((key, value) in rawData) {
                    val period = getPeriodFromString(key)
                    if (period != null) {
                        val previousClose = (rawData["previous_close"] as? Number)?.toDouble()
                        val series = (value as? List<Map<String, Any>>) ?: listOf()

                        val data = processSinglePriceData(
                            asset,
                            period,
                            identifier.region,
                            identifier.assetClass,
                            series,
                            previousClose
                        )
                        if (data != null) {
                            res.getOrPut(data.asset.id) { mutableMapOf() }[period] = data
                        }
                    }
                }
            }
        }
        return res
    }

    override fun getFromJson(json: Map<String, Any>): Map<AssetId, Map<StockDataPeriods, PriceDataSeries>> {
        return json.mapValues {
            (it.value as Map<String, Any>).mapKeys {
                getPeriodFromString(it.key)!!
            }.mapValues {
                PriceDataSeries.fromJSON(it.value as Map<String, Any>, assetProvider)
            }
        }
    }

    override fun getIdentifier(data: Map<AssetId, Map<StockDataPeriods, PriceDataSeries>>): PriceDataIdentifier {
        val periods: List<StockDataPeriods> = data.values
            .map { it.keys }
            .reduce { acc, keys -> acc + keys }
            .toSet()
            .toList()
        val assets = data.keys.mapNotNull {
            assetProvider.findById(it)
        }
        return PriceDataIdentifier(assets, periods)
    }

    override fun getPath(identifier: PriceDataIdentifier?): String {
        throw NotImplementedError("Not implemented yet")
    }

    override fun toJson(data: Map<AssetId, Map<StockDataPeriods, PriceDataSeries>>): Map<String, Any> {
        return data.mapValues {
            it.value.mapValues {
                it.value.toJson()
            }
        }
    }


}

val List<TimeSeriesDataPoint>.initialValue: Double
    get() = if (isNotEmpty()) first().value else 0.0

class PriceDataIdentifier(val assets: List<Asset>, val periods: List<StockDataPeriods>) {

    val region: Region
        get() {
            if (assets.isEmpty()) {
                throw IllegalArgumentException("Assets list must not be empty")
            }
            val region = assets.first().region
            if (assets.any { it.region != region }) {
                throw IllegalArgumentException("All assets must be in the same region")
            }
            return region
        }

    val assetClass: AssetClass
        get() {
            if (assets.isEmpty()) {
                throw IllegalArgumentException("Assets list must not be empty")
            }
            val assetClass = assets.first().assetClass
            if (assets.any { it.assetClass != assetClass }) {
                throw IllegalArgumentException("All assets must be in the same asset class")
            }
            return assetClass
        }
}
