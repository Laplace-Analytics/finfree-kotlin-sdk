package repositories

import MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.logger
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.AssetClass
import sdk.models.data.assets.Region
import sdk.models.core.Sessions
import sdk.models.data.assets.string
import sdk.repositories.SessionsRepo
import sdk.repositories.SessionsRepoIdentifier

class SessionsRepoTest{
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var sessionsRepo: SessionsRepo
    private lateinit var regionListWithoutTest: List<Region>


    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        regionListWithoutTest = Region.values().filter { it != Region.Test }


        sessionsRepo = SessionsRepo(
            MockStorage(),
            coreApiProvider
        ) { "Europe/Istanbul" };
    }
    @Test
    fun `Basic usage test`() = runBlocking {
        val sessions: List<Sessions>? = sessionsRepo.fetchData(null)
        assertNull(sessions)

        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        val sessions2: List<Sessions>? = sessionsRepo.fetchData(null)
        assertNotNull(sessions2)
        assertFalse(sessions2.isNullOrEmpty())

        val sessions3: List<Sessions>? = sessionsRepo.fetchData(
            SessionsRepoIdentifier(Region.Turkish, AssetClass.Crypto)
        )
        assertNotNull(sessions3)
        assertFalse(sessions3.isNullOrEmpty())

        val sessions4: List<Sessions>? = sessionsRepo.fetchData(
            SessionsRepoIdentifier(Region.American)
        )
        assertNotNull(sessions4)
        assertFalse(sessions4.isNullOrEmpty())

        val sessions5: List<Sessions>? = sessionsRepo.fetchData(
            SessionsRepoIdentifier(assetClass = AssetClass.Crypto)
        )
        assertNotNull(sessions5)
        assertFalse(sessions5.isNullOrEmpty())

        val sessions6: List<Sessions>? = sessionsRepo.fetchData(
            SessionsRepoIdentifier(assetClass = AssetClass.Forex)
        )
        assertNull(sessions6)
    }

    @Test
    fun `Fetch specific region and assetClass Test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        val sessionTestRegionList = regionListWithoutTest
        val sessionTestAssetClassList = AssetClass.values().toList()

        for (region in sessionTestRegionList) {
            for (assetClass in sessionTestAssetClassList) {
                val sessions: List<Sessions>? = sessionsRepo.fetchData(
                    SessionsRepoIdentifier(region, assetClass)
                )
                if (sessions.isNullOrEmpty()) {
                    logger.error("UNDEFINED OR ERROR FOR GET SESSIONS: ${region.string()}/${assetClass.string()}")
                    break
                }
                for (session in sessions) {
                    assertEquals(session.region, region)
                    assertEquals(session.assetClass, assetClass)
                }
            }
        }
    }


}