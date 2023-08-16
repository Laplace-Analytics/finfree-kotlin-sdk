package sdk.trade

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*

class DriveWealthAccountApiProvider(
    override val httpHandler: HTTPHandler,
    val basePath:String
): GenericApiProvider(httpHandler){

    suspend fun getDriveWealthStatements(): BasicResponse<List<Map<String, Any>>> {
        val path:String = "$basePath/account/statements"
        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = {
                res ->
                val data = Json.decodeFromString<List<Map<String, Any>>>(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data
                )
            },
            onError = {
                res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun postGedikUSWithdraw(amount:Double,iban:String): BasicResponse<*> {
        val path:String = "$basePath/fund/withdrawal"
        val response = httpHandler.post(
            path = path,
            body = Json.encodeToString(
                mapOf(
                    "amount" to amount,
                    "iban" to iban,
                    "type" to "INSTANT_FUNDING",
                    "currency" to "USD"
                )
            )
        )

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                )
            }
        ) as BasicResponse
    }

    suspend fun getGedikUSSavedIBANList(): BasicResponse<List<String>> {
        val path:String = "$basePath/account/ibans"
        val response = httpHandler.get(path = path)

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data: Map<String, Any>  = Json.decodeFromString(res.body!!.string())
                val ibans: List<String> = data["ibans"] as List<String>
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = ibans
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<List<String>>
    }

    suspend fun getFinveoDepositToken(amount: Int): BasicResponse<String> {
        val path:String = "$basePath/fund/tokenize"
        val response = httpHandler.get(
            path = path,
            data = mapOf(
                "amount" to amount.toString(),
                "currency" to "USD"
            )
        )
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data: Map<String, Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data["token"] as String
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<String>
    }

    suspend fun getFinveoDepositUrl(token: String): BasicResponse<String> {
        val path:String = "$basePath/fund/url"
        val response = httpHandler.get(
            path = path,
            data = mapOf(
                "token" to token
            )
        )
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data: Map<String, Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data["url"] as String
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<String>
    }
}