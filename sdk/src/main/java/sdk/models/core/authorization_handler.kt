package sdk.models.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.api.*
import sdk.base.GenericStorage
import sdk.base.network.HTTPHandler
import sdk.models.core.FinfreeSDK.Companion.setAccessToken

class AuthorizationHandler(
    private val storage: GenericStorage,
    private val httpHandler: HTTPHandler
) {
    private val authApiProvider = AuthApiProvider(httpHandler)
    private val authPath = "auth/login_data"

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
                    authPath,
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

    suspend fun getFinfreeLoginData(): LoginResponseData? {
        val savedLoginDataJson = storage.read(authPath)
        return if (savedLoginDataJson != null) {
            val savedLoginData = Json.decodeFromString<Map<String, Any>>(savedLoginDataJson)
            LoginResponseData.fromJson(savedLoginData)
        } else {
            null
        }
    }

    suspend fun authenticateWithRefreshToken(refreshToken: RefreshToken? = null, tokenId: String? = null): AuthenticationResponse {

        var refreshTokenToUse: String? = null
        var tokenIdToUse: String? = null

        if (refreshToken != null && tokenId != null) {
            refreshTokenToUse = refreshToken
            tokenIdToUse = tokenId
        } else {
            val savedLoginDataJson: String? = storage.read(authPath)

            if (savedLoginDataJson != null) {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = Gson().fromJson(savedLoginDataJson, type)
                val savedLogin = LoginResponseData.fromJson(data)
                refreshTokenToUse = savedLogin.refreshToken
                tokenIdToUse = savedLogin.tokenId
            }
        }

        if (refreshTokenToUse == null || tokenIdToUse == null) {
            return AuthenticationResponse(AuthenticationResponseTypes.UnknownError, "Unknown error", null)
        }

        val response = authApiProvider.getAccessToken(
            refreshToken = refreshTokenToUse,
            tokenId = tokenIdToUse,
        )

        return if (response.data != null) {
            val loginData = LoginResponseData(
                refreshToken = refreshTokenToUse,
                tokenId = tokenIdToUse,
                accessToken = response.data
            )

            storage.save(
                authPath,
                Json.encodeToString(loginData.toJson())
            )

            return AuthenticationResponse(
                AuthenticationResponseTypes.Success,
                null,
                response.data
            )
        } else {
            AuthenticationResponse(response.responseType.authenticationResponseType, response.message, null)
        }
    }

     fun logout(){
         storage.clearFile(
             authPath
         )
     }



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
