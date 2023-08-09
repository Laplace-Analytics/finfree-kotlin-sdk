package sdk.api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sdk.base.network.*

class StockDataApiProvider(
    override val httpHandler: HTTPHandler,
    private val basePath: String
) : GenericApiProvider(httpHandler) {

    suspend fun getStockData(
        locale: String,
        assetClass: String,
        symbols: List<String>,
        fields: List<StockDataPeriods>
    ): BasicResponse<List<Map<String, Any>>> {
        val path = "$basePath/$assetClass/$locale"
        val fieldList = fields.map { it.period }.toMutableList()
        if (fieldList.contains(StockDataPeriods.Price1D.period) && assetClass != "crypto") {
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
                val responseBody = Json.decodeFromString<Map<String, Any>>(res.body!!.string())
                BasicResponse(
                    data = listOf(responseBody) ?: emptyList(),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun getStockStats(assetClass: String, symbol: String): BasicResponse<Map<String, Any>> {
        val path = "stock/stats/$assetClass/$symbol"

        val response = httpHandler.get(path = path, tryAgainOnTimeout = false)

        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<Map<String, Any>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            },
            onError = { res ->
                BasicResponse(
                    data = null,
                    responseType = BasicResponseTypes.Error,
                    message = "${res.body} - ${res.code} - ${res.request} - ${res.message}"
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

}

enum class StockDataPeriods(val period: String, val periodMinutes: Int) {
    Price1D("1G", 5),
    Price1W("1H", 30),
    Price1M("1A", 120),
    Price3M("3A", 360),
    Price1Y("1Y", 1440),
    Price5Y("5Y", 7200),
    PriceAllTime("all_time", 60);
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


