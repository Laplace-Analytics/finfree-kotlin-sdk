package sdk.trade.repositories.repos

import sdk.api.StockDataPeriods
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.data.assets.AssetId
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import sdk.models.data.time_series.EquityDataPoint
import sdk.models.data.time_series.UserEquityTimeSeries
import sdk.repositories.PriceDataRepo
import sdk.repositories.initialValue
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.OrderData
import sdk.trade.models.portfolio.PortfolioAssetData
import sdk.trade.models.portfolio.UserEquityData
import java.time.Duration
import java.time.LocalDateTime

abstract class UserEquityRepo<T : GenericPortfolioApiProvider>(
    storageHandler: GenericStorage,
    apiProvider: T,
    val priceDataRepo: PriceDataRepo,
    val sessionProvider: SessionProvider,
    val assetProvider: AssetProvider
) : GenericRepository<UserEquityData, PortfolioRepoIdentifier,  T>(storageHandler, apiProvider)

data class PortfolioRepoIdentifier(
    val orderData: List<OrderData>,
    val portfolioAssets: Map<AssetId, PortfolioAssetData>,
    val livePriceDataEnabled: Boolean = false
)

class EquityTimeSeriesRepo<T : GenericPortfolioApiProvider>(
    private val sessionProvider: SessionProvider,
    storageHandler: GenericStorage,
    apiProvider: T
) : GenericRepository<UserEquityTimeSeries, StockDataPeriods, T>(storageHandler, apiProvider) {

    override suspend fun fetchData(identifier: StockDataPeriods?): UserEquityTimeSeries? {
        if (identifier == null) return null
        try {
            val fetchedEquityDataJson = apiProvider.getEquityData(identifier)
            val fetchedEquityData = if (fetchedEquityDataJson.responseType != BasicResponseTypes.Success) {
                null
            } else {
                getFromJson(
                    mapOf(
                        "data" to (fetchedEquityDataJson.data ?: emptyList()),
                        "period" to identifier
                    )
                )
            }
            return fetchedEquityData
        } catch (err: Exception) {
            logger.error("Error fetching single period", err)
            throw Exception("fetchAndCacheSinglePeriod failed.")
        }
    }

    override fun getFromJson(json: Map<String, Any>): UserEquityTimeSeries {
        val data = json["data"] as List<Map<String, Any>>
        val period = json["period"] as StockDataPeriods
        val cleanData = toTimeSeriesDataPointList(rawData = data)
        val trimmedData = adjustInitialPoint(cleanData = cleanData, period = period)
        val initialValue = if (period == StockDataPeriods.Price1D) {
            (json["previous_close"] as? Double) ?: trimmedData.initialValue
        } else {
            trimmedData.initialValue
        }
        return UserEquityTimeSeries(trimmedData,initialValue, period)
    }

    private fun toTimeSeriesDataPointList(rawData: List<Map<String, Any>>?): List<EquityDataPoint> {
        if (rawData.isNullOrEmpty()) {
            return emptyList()
        }

        rawData.sortedBy { it["d"] as Int }

        var index = 0
        if (rawData.first().containsKey("aum")) {
            val firstRealizedReturn = rawData.first()["realized_return"] as Double
            val firstUnrealizedReturn = rawData.first()["unrealized_return"] as Double
            return rawData.map { dataPoint ->
                index += 1
                EquityDataPoint(
                    index = index,
                    timeStamp = dataPoint["d"] as Long,
                    value = dataPoint["indexed"] as Double,
                    assetsUnderManagement = dataPoint["aum"] as Double,
                    realizedReturn = (dataPoint["realized_return"] as Double) - firstRealizedReturn,
                    unrealizedReturn = (dataPoint["unrealized_return"] as Double) - firstUnrealizedReturn
                )
            }
        } else {
            return rawData.map { dataPoint ->
                index += 1
                EquityDataPoint(
                    index = index,
                    timeStamp = dataPoint["d"] as Long,
                    value = dataPoint["e"] as Double,
                    assetsUnderManagement = dataPoint["e"] as Double,
                    realizedReturn = 0.0,
                    unrealizedReturn = (dataPoint["e"] as Double) - (rawData.first()["e"] as Double)
                )
            }
        }
    }


    private fun getPeriodStartDate(period: StockDataPeriods, lastDataPoint: LocalDateTime): LocalDateTime {
        val periodStartDate: LocalDateTime = when (period) {
            StockDataPeriods.Price1D -> {
                LocalDateTime.of(lastDataPoint.year, lastDataPoint.month, lastDataPoint.dayOfMonth, 10, 0, 0)
            }
            StockDataPeriods.Price1W -> {
                lastDataPoint.minus(Duration.ofDays(5))
            }
            StockDataPeriods.Price1M -> {
                lastDataPoint.minusMonths(1).plusDays(2)
            }
            StockDataPeriods.Price3M -> {
                lastDataPoint.minusMonths(3).plusDays(2)
            }
            StockDataPeriods.Price1Y -> {
                lastDataPoint.minusYears(1).plusDays(2)
            }
            StockDataPeriods.Price5Y -> {
                lastDataPoint.minusYears(5).plusDays(2)
            }
            else -> {
                lastDataPoint.minus(Duration.ofDays(1))
            }
        }

        return sessionProvider.getNextTradingDay(date = periodStartDate)
    }


    private fun adjustInitialPoint(cleanData: List<EquityDataPoint>, period: StockDataPeriods): List<EquityDataPoint> {
        if (cleanData.isEmpty()) {
            return cleanData
        }
        val periodStartDate = getPeriodStartDate(period, cleanData.last().time)
        val modifiedData = cleanData.toMutableList()
        while (cleanData.first().time.isAfter(periodStartDate)) {
            modifiedData.add(0, EquityDataPoint(
                index = cleanData.first().index - 1,
                timeStamp = cleanData.first().time.minus(Duration.ofMinutes(period.periodMinutes.toLong())).toEpochMilliSecond(),
                value = cleanData.first().value,
                assetsUnderManagement = cleanData.first().assetsUnderManagement,
                realizedReturn = cleanData.first().realizedReturn,
                unrealizedReturn = cleanData.first().unrealizedReturn
            ))
        }
        return modifiedData
    }

    override fun getIdentifier(data: UserEquityTimeSeries): StockDataPeriods {
        return data.period
    }

    override fun getPath(identifier: StockDataPeriods?): String {
        if (identifier == null) throw Exception("Identifier cannot be null")
        return "equity_graph/${identifier.period}"
    }

    override fun toJson(data: UserEquityTimeSeries): Map<String, Any> {
        // Implement this method as needed
        throw UnsupportedOperationException("Not implemented yet")
    }
}

