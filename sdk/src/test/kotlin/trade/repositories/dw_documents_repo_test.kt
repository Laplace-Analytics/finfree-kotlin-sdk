package trade.repositories

import sdk.base.MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.network.HTTPHandler
import sdk.trade.DriveWealthAccountApiProvider
import sdk.trade.DriveWealthDocumentTypes
import sdk.trade.models.portfolio.DriveWealthAccountDocument
import sdk.trade.repositories.drivewealth_repos.DriveWealthDocumentsRepo

class DriveWealthDocumentsRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var driveWealthAccountApiProvider: DriveWealthAccountApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthDocumentsRepo: DriveWealthDocumentsRepo

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        driveWealthHttpHandler = HTTPHandler(httpURL = "drivewealth.finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        driveWealthAccountApiProvider = DriveWealthAccountApiProvider(
            driveWealthHttpHandler,
            "api/v1/eu"
        )
        driveWealthDocumentsRepo = DriveWealthDocumentsRepo(
            MockStorage(),
            driveWealthAccountApiProvider
        )
    }

    @Nested
    inner class GetDocumentsTests{
        @Test
        fun `Get data test`() = runBlocking{
            val loginResponse = authApiProvider.postLogin("", "")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken

            for (documentType in DriveWealthDocumentTypes.values()) {
                 val documents: List<DriveWealthAccountDocument>? = driveWealthDocumentsRepo.getData(documentType)
                assertNotNull(documents)
                assertTrue(documents is List)
                assertTrue(documents!!.isNotEmpty())
            }
        }
        }
    }