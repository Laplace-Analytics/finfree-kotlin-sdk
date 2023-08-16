package sdk.trade.generic_api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sdk.base.network.ApiResponseHandler
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.base.network.HTTPHandler
import sdk.trade.GenericPortfolioApiProvider

class DriveWealthPortfolioApiProvider(
    override val httpHandler: HTTPHandler,
    override val basePath: String

): GenericPortfolioApiProvider(httpHandler, basePath) {
    override suspend fun getPortfolio(): BasicResponse<Map<String, Any>> {
        val path = "$basePath/account/portfolio"
        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data: Map<String, Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    override suspend fun getEquityData(period: String): BasicResponse<List<Map<String, Any>>> {
        val path = "$basePath/account/portfolio/history"
        val params:Map<String,String> = mapOf(
            "type" to period
        )

        val response = httpHandler.get(path = path, data = params, tryAgainOnTimeout = false)

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data:List<Map<String,Any>>?  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data ?: emptyList(),
                    message = null
                )
            },

        ) as BasicResponse<List<Map<String, Any>>>
    }
}