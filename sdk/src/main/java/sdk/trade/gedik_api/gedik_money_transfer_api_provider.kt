package sdk.trade

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*

class GedikMoneyTransferApiProvider(
    override val httpHandler: HTTPHandler,
    val basePath: String,
): GenericApiProvider(httpHandler){
    suspend fun postWithdraw(amount:Double,iban:String): BasicResponse<*> {
        val response = httpHandler.post(
            path = "$basePath/withdraw",
            body = Json.encodeToString(
                mapOf(
                    "amount" to  0 - amount,
                    "iban" to  iban,
                    "type" to  "INSTANT_FUNDING",
                    "currency" to  "USD",
                )
            )
        )
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = {
                BasicResponse(
                    responseType = BasicResponseTypes.Success
                )
            }
        ) as BasicResponse
    }
}