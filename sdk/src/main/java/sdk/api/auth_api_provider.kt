package sdk.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.ApiResponseHandler
import sdk.base.network.ApiResponseObject
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.base.network.GenericApiProvider
import sdk.base.network.HTTPHandler

 class AuthApiProvider(
    override val httpHandler: HTTPHandler
    ) : GenericApiProvider(httpHandler) {

     suspend fun getAccountData(
         requestedFields: List<String> = listOf(
             "username",
             "email",
             "phone",
             "first_name",
             "last_name",
             "referral_code",
             "referral_points",
             "referred_code",
             "referred_users",
             "account_id",
             "dw_account_id",
             "dw_account_no"
         )
     ): BasicResponse<Map<String, Any>> {

         val path = "account"
         val data = mapOf("fields" to requestedFields.joinToString(","))

         val response = httpHandler.get(
             path = path,
             data = data,
             tryAgainOnTimeout = true
         )

         return ApiResponseHandler.handleResponse(
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
    suspend fun postLogin(identifier: String, password: String): LoginResponse{
        val response = httpHandler.post(
            path = "v3/login",
            body = Json.encodeToString(
                mapOf(
                    "identifier" to identifier,
                    "password" to password
                )
            )
        )
        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val resultMap: Map<String, Any> = Gson().fromJson(responseBodyStr,type)

                val accessToken = resultMap["access_token"] as String
                val refreshToken = resultMap["refresh_token"] as String
                val tokenId = resultMap["token_id"] as String

                val data = LoginResponseData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    tokenId = tokenId
                )

                 LoginResponse(
                    data = data,
                    responseType = LoginResponseTypes.SUCCESS,
                    message = responseBodyStr
                )
            },
            onUnauthorized = { res ->
                val responseBodyStr = res.body?.string()

                LoginResponse(
                    responseType = LoginResponseTypes.UNAUTHORIZED,
                    message = responseBodyStr
                )
            },
            onServerError = { res ->
                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<String>() {}.type

                val result: String = Gson().fromJson(responseBodyStr,type)
                val responseBody = try {
                    result
                } catch (e: Exception) {
                    null
                }
                LoginResponse(
                    data = null,
                    responseType = LoginResponseTypes.ERROR,
                    message = responseBody
                )
            },
            onError = {
                LoginResponse(
                    data = null,
                    responseType = LoginResponseTypes.ERROR,
                    message = "error"
                )
            }
        ) as LoginResponse
    }

    suspend fun getAccessToken(refreshToken: String, tokenId: String): AccessTokenResponse {
        val response = httpHandler.post(
            path = "v3/refresh",
            body = Json.encodeToString(
                mapOf(
                    "refresh_token" to refreshToken,
                    "token_id" to tokenId
                )
            )
        )



        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res ->
                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, String>>() {}.type
                val resultMap: Map<String, String> = Gson().fromJson(responseBodyStr,type)
                val accessToken = resultMap["access_token"] ?: ""
                AccessTokenResponse(
                    data = accessToken,
                    responseType = AccessTokenResponseTypes.Success
                )
            },
            onServerError = {
                AccessTokenResponse(
                    data = null,
                    responseType = AccessTokenResponseTypes.ServerError
                )
            },
            onImATeapot = {
                AccessTokenResponse(
                    data = null,
                    responseType = AccessTokenResponseTypes.ServerError
                )
            },
            onClientError = {
                AccessTokenResponse(
                    data = null,
                    responseType = AccessTokenResponseTypes.ClientError
                )
            }
        ) as AccessTokenResponse
    }

}
enum class LoginResponseTypes {
    SUCCESS, UNAUTHORIZED, ERROR
}
data class LoginResponse(
    override val data: LoginResponseData? = null,
    override val responseType: LoginResponseTypes,
    override val message: String? = null
) : ApiResponseObject<LoginResponseData, LoginResponseTypes>

typealias AccessToken = String
typealias RefreshToken = String

data class LoginResponseData(
    val accessToken : AccessToken,
    val refreshToken : RefreshToken,
    val tokenId : String
){
    fun toJson(): Map<String,Any> = mapOf(
        "token_id" to tokenId,
        "refresh_token" to refreshToken,
        "access_token" to accessToken
    )
    companion object{
        fun fromJson(json: Map<String,Any>): LoginResponseData {
            return LoginResponseData(
                tokenId = json["token_id"] as String,
                refreshToken = json["refresh_token"] as String,
                accessToken = json["access_token"] as String
            )
        }
    }
}

enum class AccessTokenResponseTypes {
    Success,
    ServerError,
    ClientError
}

data class AccessTokenResponse(
    override val data: String? = null,
    override val responseType: AccessTokenResponseTypes,
    override val message: String? = null
) : ApiResponseObject<String, AccessTokenResponseTypes>
