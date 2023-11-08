package trade.repositories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.MockStorage
import sdk.base.network.HTTPHandler
import sdk.trade.DriveWealthAccountApiProvider
import sdk.trade.models.portfolio.DriveWealthViolationsData
import sdk.trade.repositories.drivewealth_repos.DriveWealthViolationsRepo

class DriveWealthViolationsRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var driveWealthAccountApiProvider: DriveWealthAccountApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthViolationsRepo: DriveWealthViolationsRepo

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        driveWealthHttpHandler = HTTPHandler(httpURL = "drivewealth.finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        driveWealthAccountApiProvider = DriveWealthAccountApiProvider(driveWealthHttpHandler, "api/v1/eu");
        driveWealthViolationsRepo = DriveWealthViolationsRepo(MockStorage(), driveWealthAccountApiProvider);
    }

    @Nested
    inner class DriveWealthUserPortfolioRepoTests{
        @Test
        fun `Get violations tests`() = runBlocking{
            val loginResponse = authApiProvider.postLogin("", "")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken

                val violationsData: DriveWealthViolationsData? = driveWealthViolationsRepo.getData(null)
                assertNotNull(violationsData)
                assertTrue(violationsData!!.goodFaithViolationCount < 4)
                assertNotNull(violationsData.goodFaithViolationCount)
        }
    }
}