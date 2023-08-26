package sdk.models.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.api.*
import sdk.base.GenericStorage
import sdk.base.network.HTTPHandler

class AuthorizationHandler(
    private val storage: GenericStorage,
    private val httpHandler: HTTPHandler
) {
    private val authApiProvider = AuthApiProvider(httpHandler)
    private val _authPath = "auth/login_data"

    suspend fun login(identifier: String, password: String): AuthenticationResponse {
        val authenticationResponse = authenticateWithRefreshToken()

        if (authenticationResponse.responseType == AuthenticationResponseTypes.Success) {
            return authenticationResponse
        }

        val response = authApiProvider.postLogin(identifier, password)

        when (response.responseType) {
            LoginResponseTypes.SUCCESS -> {
                if(response.data?.accessToken == null){
                    return AuthenticationResponse(AuthenticationResponseTypes.UnknownError, response.message, null)
                }

                val accessToken = response.data.accessToken

                storage.save(
                    _authPath,
                    Gson().toJson(
                        response.data.toJson()
                    )
                )

                setAccessToken(accessToken)
                return AuthenticationResponse(
                    AuthenticationResponseTypes.Success,
                    null,
                    accessToken
                )
            }
            LoginResponseTypes.UNAUTHORIZED -> {
                return AuthenticationResponse(
                    AuthenticationResponseTypes.Unauthorized,
                    response.message,
                    null
                )
            }
            LoginResponseTypes.ERROR -> {
                return AuthenticationResponse(
                    AuthenticationResponseTypes.UnknownError,
                    response.message,
                    null
                )
            }
        }
    }
    suspend fun authenticateWithRefreshToken(): AuthenticationResponse {
        val savedLoginDataJson = storage.read(_authPath)

        savedLoginDataJson?.let {

            val type = object : TypeToken<Map<String, Any>>() {}.type

            val data: Map<String, Any> = Gson().fromJson(savedLoginDataJson,type)
            val savedLogin = LoginResponseData.fromJson(data)

            val response = authApiProvider.getAccessToken(
                refreshToken = savedLogin.refreshToken,
                tokenId = savedLogin.tokenId
            )

            if(response.data != null){
                setAccessToken(response.data)
                return AuthenticationResponse(AuthenticationResponseTypes.Success, null, response.data)
            }

            return AuthenticationResponse(response.responseType.authenticationResponseType, response.message, null)
        }

        return AuthenticationResponse(AuthenticationResponseTypes.UnknownError, "Unknown error", null)
    }

    private fun setAccessToken(accessToken: AccessToken) { }

}
enum class AuthenticationResponseTypes {
    Success,
    Unauthorized,
    ServerError,
    UnknownError,
}

data class AuthenticationResponse(
    val responseType: AuthenticationResponseTypes,
    val reasonMessage: String?,
    val accessToken: AccessToken?
)

val AccessTokenResponseTypes.authenticationResponseType: AuthenticationResponseTypes
    get() {
        return when (this) {
            AccessTokenResponseTypes.Success -> AuthenticationResponseTypes.Success
            AccessTokenResponseTypes.ServerError -> AuthenticationResponseTypes.ServerError
            AccessTokenResponseTypes.ClientError -> AuthenticationResponseTypes.UnknownError
        }
    }
