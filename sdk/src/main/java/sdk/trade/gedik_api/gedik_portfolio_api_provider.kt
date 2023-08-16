package sdk.trade.gedik_api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sdk.base.network.*

class GedikPortfolioApiProvider(
    override val httpHandler: HTTPHandler,
    private val basePath: String
) : GenericApiProvider(httpHandler) {

    private var testAPI: Boolean = false

    val pathTestExtension: String
        get() = if (testAPI) "test" else ""

    fun enableTestAPI() {
        testAPI = true
    }

    suspend fun getBalanceInfo() : BasicResponse<Map<String, Any>> {
        val path: String = "execute$pathTestExtension/midserver/balanceinfo"
        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<Map<String, Any>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    suspend fun getPortfolio() : BasicResponse<Map<String, Any>> {
        val path: String = "execute$pathTestExtension/v2/portfolio"
        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)
        return ApiResponseHandler.handleResponse(
            response,
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
                    message = "${response.body} - ${response.code} - ${response.request}"
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    suspend fun getMarketTradeTypes() : BasicResponse<List<Map<String, Any>>> {
        val path: String = "execute$pathTestExtension/stocks/market_type"
        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                BasicResponse(
                    data = Json.decodeFromString<List<Map<String, Any>>>(res.body!!.string()),
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun getEquityData(period:String) : BasicResponse<List<Map<String, Any>>> {
        val path: String = "execute/graph/$period"
        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val body = Json.decodeFromString<List<Map<String, Any>>>((res.body!!.string()))
                BasicResponse(
                    data = body.let { it.map { item -> item.toMap() } },
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }


}
