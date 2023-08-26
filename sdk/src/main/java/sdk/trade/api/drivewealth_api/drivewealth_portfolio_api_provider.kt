package sdk.trade.generic_api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sdk.api.StockDataPeriods
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val resultMap: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = resultMap
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    override suspend fun getEquityData(period: StockDataPeriods): BasicResponse<List<Map<String, Any>>> {
        val path = "$basePath/account/portfolio/history"
        val params:Map<String,String> = mapOf(
            "type" to period.period
        )

        val response = httpHandler.get(path = path, data = params, tryAgainOnTimeout = false)

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val resultMap: List<Map<String, Any>>? = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = resultMap ?: emptyList(),
                    message = null
                )
            },

        ) as BasicResponse<List<Map<String, Any>>>
    }
}