package api
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AccessTokenResponse
import sdk.api.AccessTokenResponseTypes
import sdk.api.AuthApiProvider
import sdk.api.LoginResponse
import sdk.api.LoginResponseData
import sdk.api.LoginResponseTypes
import sdk.base.network.HTTPHandler
import sdk.models.core.sessions.DateTime
import java.util.Base64
import kotlin.text.Charsets.UTF_8

private fun decodeToken(token: String): Map<String, Any> {
    val tokenCredentials = token.split('.')[1]
    val type = object : TypeToken<Map<String, Any>>() {}.type


    val normalizedSource = String(Base64.getDecoder().decode(tokenCredentials), UTF_8)
    val resultMap: Map<String, Any> = Gson().fromJson(normalizedSource,type)


    return resultMap
}

class AuthenticationApiProviderTests {

    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler

    private val testLoginResponses = mutableListOf<LoginResponse>()
    private val testAccessTokenResponses = mutableListOf<AccessTokenResponse>()

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
    }

    @Test
    fun `loginTest`() {
        runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "wrongPassword")
            testLoginResponses.add(loginResponse)

            assertEquals(LoginResponseTypes.UNAUTHORIZED, loginResponse.responseType)
            assertNull(loginResponse.data)
            assertEquals("Unauthorized\n", loginResponse.message)

            val loginResponse2 = authApiProvider.postLogin("test44", "1234qwer")
            testLoginResponses.add(loginResponse2)

            assertEquals(LoginResponseTypes.SUCCESS, loginResponse2.responseType)
            assertNotNull(loginResponse2.data)
            assertTrue(loginResponse2.data is LoginResponseData)
            assertNotNull(loginResponse2.data?.accessToken)

            val decodedJWTToken = decodeToken(loginResponse2.data?.accessToken ?: "")
            assertNotNull(decodedJWTToken["username"])
        }
    }
    @Test
    fun `successScenario`() {
        runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            testLoginResponses.add(loginResponse)

            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            val accessTokenResponse = authApiProvider.getAccessToken(
                refreshToken = loginData.refreshToken,
                tokenId = loginData.tokenId
            )
            testAccessTokenResponses.add(accessTokenResponse)

            assertEquals(AccessTokenResponseTypes.Success, accessTokenResponse.responseType)
            assertTrue(accessTokenResponse.data is String)
            assertNotNull(accessTokenResponse.data)
            val decodedJWTToken = decodeToken(accessTokenResponse.data ?: "")
            assertNotNull(decodedJWTToken["username"])
        }

    }

    @Test
    fun `wrongOrExpiredTokenScenario`() {
        runBlocking {

            val accessTokenResponse = authApiProvider.getAccessToken(
                refreshToken = "wrongOrExpiredRefreshToken",
                tokenId = "tokenId"
            )
            testAccessTokenResponses.add(accessTokenResponse)
            assertEquals(AccessTokenResponseTypes.ClientError, accessTokenResponse.responseType)
            assertNull(accessTokenResponse.data)

            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            testLoginResponses.add(loginResponse)

            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            val accessTokenResponse2 = authApiProvider.getAccessToken(
                refreshToken = loginData.refreshToken,
                tokenId = loginData.tokenId
            )
            testAccessTokenResponses.add(accessTokenResponse2)
            assertEquals(AccessTokenResponseTypes.Success, accessTokenResponse2.responseType)
            assertNotNull(accessTokenResponse2.data)

            val decodedJWTToken = decodeToken(accessTokenResponse2.data ?: "")
            val accessTokenExpireDate = DateTime.fromSinceEpochMilliSecond((decodedJWTToken["exp"] as Double * 1000).toLong())
            assertNotNull(decodedJWTToken["username"])
            assertTrue(accessTokenExpireDate.isAfter(DateTime.now()))
        }
    }
}