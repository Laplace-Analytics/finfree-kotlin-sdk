package sdk.trade

import sdk.api.StockDataPeriods
import sdk.base.network.BasicResponse
import sdk.base.network.GenericApiProvider
import sdk.base.network.HTTPHandler

abstract class GenericPortfolioApiProvider(
    override val httpHandler: HTTPHandler,
    open val basePath: String
): GenericApiProvider(httpHandler){

    abstract suspend fun getPortfolio(): BasicResponse<Map<String,Any>>
    abstract suspend fun getEquityData(period:StockDataPeriods): BasicResponse<List<Map<String,Any>>>
}

