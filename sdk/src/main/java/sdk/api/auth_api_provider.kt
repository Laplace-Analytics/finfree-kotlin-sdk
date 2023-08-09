package sdk.api

import com.sun.net.httpserver.HttpHandler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.ApiResponseHandler
import sdk.base.network.ApiResponseObject
import sdk.base.network.GenericApiProvider
import sdk.base.network.HTTPHandler

class AuthApiProvider(
    override val httpHandler: HTTPHandler
    ) : GenericApiProvider(httpHandler) {

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
                val responseBody = Json.decodeFromString<Map<String, Any>>(res.body!!.string())
                val accessToken = responseBody["access_token"] as String
                val refreshToken = responseBody["refresh_token"] as String
                val tokenId = responseBody["token_id"] as String

                val data = LoginResponseData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    tokenId = tokenId
                )
                 LoginResponse(
                    data = data,
                    responseType = LoginResponseTypes.SUCCESS,
                    message = res.body?.string()
                )
            },
            onUnauthorized = { res ->
                val responseBody = try {
                    Json.decodeFromString<String>(res.body!!.string())
                } catch (e: Exception) {
                    null
                }
                LoginResponse(
                    responseType = LoginResponseTypes.UNAUTHORIZED,
                    message = responseBody
                )
            },
            onServerError = { res ->
                val responseBody = try {
                    Json.decodeFromString<String>(res.body!!.string())
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
                val accessToken = Json.decodeFromString<Map<String, String>>(res.body!!.string())["access_token"] ?: ""
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
