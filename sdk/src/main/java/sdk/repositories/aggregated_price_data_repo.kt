package sdk.repositories

import sdk.api.StockDataApiProvider
import sdk.api.StockDataPeriods
import sdk.base.GenericRepository
import sdk.base.GenericStorage
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.models.Asset
import sdk.models.AssetClass
import sdk.models.PriceDataPoint
import sdk.models.PriceDataSeries
import sdk.models.Region
import java.time.Duration
import java.time.LocalDateTime

open class AggregatedPriceDataSeriesRepo(
    override  val apiProvider: StockDataApiProvider,
    override  val storageHandler: GenericStorage,
    val priceDataRepo: PriceDataRepo,
    cacheDuration: Duration = Duration.ofMinutes(1),
    ): GenericRepository<AggregatedPriceDataSeries, AggregatedPriceDataSeriesIdentifier, StockDataApiProvider>(apiProvider = apiProvider, storageHandler = storageHandler, cacheDuration = cacheDuration){
    override suspend fun fetchData(identifier: AggregatedPriceDataSeriesIdentifier?): AggregatedPriceDataSeries? {
        if (identifier == null) {
            return null
        }

        val assets: List<Asset> = identifier.assets
        val period: StockDataPeriods = identifier.period
        val regionAssetMapping: MutableMap<Region, MutableMap<AssetClass, MutableList<Asset>>> = mutableMapOf()

        for (asset in assets) {
            if (regionAssetMapping[asset.region] == null) {
                regionAssetMapping[asset.region] = mutableMapOf()
            }
            if (regionAssetMapping[asset.region]?.get(asset.assetClass) == null) {
                regionAssetMapping[asset.region]?.put(asset.assetClass, mutableListOf())
            }
            regionAssetMapping[asset.region]?.get(asset.assetClass)?.add(asset)
        }

        val graphData: MutableMap<Region, MutableMap<AssetClass, PriceDataSeries?>> = mutableMapOf()

        for ((region, assetClassToAssetEntry) in regionAssetMapping) {
            for ((assetClass, assetList) in assetClassToAssetEntry) {
                if (graphData[region] == null) {
                    graphData[region] = mutableMapOf()
                }

                val assetSymbols: List<String> = assetList.map { it.symbol }

                val response: BasicResponse<Map<String, Any>> = apiProvider.getAggregatedPriceData(
                    symbols = assetSymbols,
                    period = period,
                    region = region,
                    assetClass = assetClass
                )

                if (response.responseType == BasicResponseTypes.Success) {
                    val priceData: PriceDataSeries? = priceDataRepo.processSinglePriceData(
                        assets.first(),
                        period,
                        region,
                        assetClass,
                        response.data!!["graph"] as List<Map<String, Any>>,
                        response.data["previous_close"].toString().toDoubleOrNull() ?: 0.0
                    )
                    graphData[region]?.put(assetClass, priceData)
                }
            }
        }

        return getFromJson(
            mapOf(
                "graph_data" to graphData,
                "assets" to assets
            )
        )
    }

    override fun getPath(identifier: AggregatedPriceDataSeriesIdentifier?): String {
        TODO("Not yet implemented")
    }

    override fun getIdentifier(data: AggregatedPriceDataSeries): AggregatedPriceDataSeriesIdentifier? {
        TODO("Not yet implemented")
    }

    override fun getFromJson(json: Map<String, Any>): AggregatedPriceDataSeries {
        val graphData: Map<Region, Map<AssetClass, PriceDataSeries?>>? = json["graph_data"] as Map<Region, Map<AssetClass, PriceDataSeries?>>?
        val assets: List<Any>? = json["assets"] as List<Any>?

        var result: PriceDataSeries? = null

        graphData?.values?.forEach { assetClassToData ->
            assetClassToData.values.forEach { priceData ->
                if (priceData != null && priceData.data.isNotEmpty()) {
                    result = if (result == null) {
                        priceData
                    } else {
                        (result!! + priceData) as PriceDataSeries
                    }
                }
            }
        }

        if (result == null) {
            return AggregatedPriceDataSeries(
                emptyList(),
                (assets?.first() as Asset),
                StockDataPeriods.Price1D,
                0.0,
                LocalDateTime.now(),
                emptyList()
            )
        }

        return AggregatedPriceDataSeries(
            result!!.data,
            result!!.asset,
            result!!.period,
            result!!.initialValue,
            result!!.lastUpdated,
            assets as List<Asset>
        )
    }

    override fun toJson(data: AggregatedPriceDataSeries): Map<String, Any> {
        return mapOf(
            "assets" to data.assets.map { asset -> asset.toJson() }.toList(),
            "data" to data.data.map { priceDataPoint -> priceDataPoint.toJson() }.toList(),
            "period" to data.period.period,
            "previousClose" to data.initialValue
        )
    }
}

class AggregatedPriceDataSeries(
    data: List<PriceDataPoint>,
    asset: Asset,
    period: StockDataPeriods,
    previousClose: Double,
    lastUpdated: LocalDateTime,
    val assets: List<Asset>
) : PriceDataSeries(data, asset, period, previousClose, lastUpdated) {

    val priceDataSeries: PriceDataSeries
        get() = PriceDataSeries(data, asset, period, initialValue, lastUpdated)
}

data class AggregatedPriceDataSeriesIdentifier(
    val assets: List<Asset>,
    val period: StockDataPeriods
)
