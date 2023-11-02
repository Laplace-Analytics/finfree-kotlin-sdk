package sdk.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import sdk.base.network.*
import sdk.models.data.assets.AssetClass
import sdk.models.data.assets.AssetId
import sdk.models.data.assets.AssetSymbol
import sdk.models.data.assets.Region
import sdk.models.data.assets.string

class StockDataApiProvider(
    override val httpHandler: HTTPHandler,
) : GenericApiProvider(httpHandler) {
    private val basePath: String = "stock"


    suspend fun getStockPriceData(
        locale: Region,
        assetClass: AssetClass,
        symbols: List<String>,
        fields: List<StockDataPeriods>
    ): BasicResponse<List<Map<String, Any>>> {
        val path = "$basePath/${assetClass.string()}/${locale.string()}"
        val fieldList = fields.map { it.period }.toMutableList()
        if (fieldList.contains(StockDataPeriods.Price1D.period) && assetClass != AssetClass.Crypto) {
            fieldList.add("previous_close")
        }


        val data = mapOf(
            "symbols" to symbols.joinToString(","),
            "keys" to fieldList.joinToString(",")
        )

        val response = httpHandler.get(path = path, data = data, tryAgainOnTimeout = false)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val result: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = result ?: emptyList(),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun getStockStatistics(
        locale: Region,
        assetClass: AssetClass,
        symbols: List<AssetSymbol>,
        fields: List<StockStatistics>
    ): StockStatisticsResponse<List<Map<String, Any>>> {
        val path = "$basePath/${assetClass.string()}/${locale.string()}"
        val fieldList = fields.map { it.slug }.toMutableList()
        if (fieldList.contains(StockDataPeriods.Price1D.period) && assetClass != AssetClass.Crypto) {
            fieldList.add("previous_close")
        }

        val data = mapOf(
            "symbols" to symbols.joinToString(","),
            "keys" to fieldList.joinToString(",")
        )

        val response = httpHandler.get(path = path, data = data, tryAgainOnTimeout = false)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val result: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)
                StockStatisticsResponse(
                    data = result,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            },
            onError = { res ->
                StockStatisticsResponse(
                    data = null,
                    responseType = BasicResponseTypes.Error,
                    message = "${res.body} - ${res.code} - ${res.request} - ${res.message}"
                )
            }
        ) as StockStatisticsResponse<List<Map<String, Any>>>
    }

    suspend fun getCryptoStatistics(
        symbol: AssetSymbol
    ): CryptoStatisticsResponse<Map<String, Any>> {
        val path = "$basePath/stats/${AssetClass.Crypto.string()}/$symbol"

        val response = httpHandler.get(path = path,tryAgainOnTimeout = false)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                CryptoStatisticsResponse(
                    data = data,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            },
            onError = { res ->
                CryptoStatisticsResponse(
                    data = null,
                    responseType = BasicResponseTypes.Error,
                    message = "${res.body} - ${res.code} - ${res.request} - ${res.message}"
                )
            }
        ) as CryptoStatisticsResponse<Map<String, Any>>
    }

    suspend fun getSimilarlyTradedStocks(symbol: AssetSymbol): SimilarlyStocksResponse<List<Map<String, Any>>> {
        val path = "stocks/similar/$symbol"

        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val data: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)
                SimilarlyStocksResponse(
                    data = data,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as SimilarlyStocksResponse<List<Map<String, Any>>>
    }

    suspend fun getStockDetail(id: AssetId): BasicResponse<Map<String, Any>> {
        val path = "stock/detail/$id"

        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse<Map<String, Any>>(
            response = response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = data,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }
    suspend fun  getAggregatedPriceData(
        symbols: List<AssetSymbol>,
        period: StockDataPeriods,
        region: Region,
        assetClass: AssetClass
    ): BasicResponse<Map<String, Any>>{

        val path = "stock/v2/aggregate/graph/${region.string()}"

        val dataMap = mapOf(
            "symbols" to symbols.joinToString(","),
            "period" to period.period,
            "asset_class" to assetClass.string()
        )

        val response = httpHandler.get(
            path = path,
            data = dataMap,
            tryAgainOnTimeout = true
        )
        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = {
                res ->
                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    data = data,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }
}

class SimilarlyStocksResponse<T>(
    override val data: T? = null,
    override val responseType: BasicResponseTypes,
    override val message: String? = null
) : ApiResponseObject<T, BasicResponseTypes>

class StockStatisticsResponse<T>(
    override val data: T? = null,
    override val responseType: BasicResponseTypes,
    override val message: String? = null
) : ApiResponseObject<T, BasicResponseTypes>

class CryptoStatisticsResponse<T>(
    override val data: T? = null,
    override val responseType: BasicResponseTypes,
    override val message: String? = null
) : ApiResponseObject<T, BasicResponseTypes>

enum class StockStatistics(val slug: String) {
    peRatio("fk"),
    pbRatio("pddd"),
    yearHigh("year_high"),
    yearLow("year_low"),
    totalShares("total_shares");
}


enum class StockDataPeriods(val period: String, val periodMinutes: Int) {
    Price1D("1G", 5),
    Price1W("1H", 30),
    Price1M("1A", 120),
    Price3M("3A", 360),
    Price1Y("1Y", 1440),
    Price5Y("5Y", 7200),
    PriceAllTime("all_time", 60);
    companion object {
        fun fromPeriodString(periodString: String): StockDataPeriods? {
            return values().find { it.period == periodString }
        }
    }
}

    fun getPeriodFromString(period: String?): StockDataPeriods? {
            return when (period) {
                "1G" -> StockDataPeriods.Price1D
                "1H" -> StockDataPeriods.Price1W
                "1A" -> StockDataPeriods.Price1M
                "3A" -> StockDataPeriods.Price3M
                "1Y" -> StockDataPeriods.Price1Y
                "5Y" -> StockDataPeriods.Price5Y
                "all_time" -> StockDataPeriods.PriceAllTime
                else -> null
            }
        }


