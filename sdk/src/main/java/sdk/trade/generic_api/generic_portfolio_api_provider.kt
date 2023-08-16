package sdk.trade

import sdk.base.network.BasicResponse
import sdk.base.network.GenericApiProvider
import sdk.base.network.HTTPHandler

abstract class GenericPortfolioApiProvider(
    override val httpHandler: HTTPHandler,
    open val basePath: String
): GenericApiProvider(httpHandler){

    abstract suspend fun getPortfolio(): BasicResponse<Map<String,Any>>
    abstract suspend fun getEquityData(period:String): BasicResponse<List<Map<String,Any>>>
}

