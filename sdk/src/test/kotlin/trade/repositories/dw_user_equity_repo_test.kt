package trade.repositories

import MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.api.StockDataApiProvider
import sdk.api.StockDataPeriods
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.Region
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.AssetRepo
import sdk.repositories.PriceDataRepo
import sdk.repositories.SessionsRepo
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.generic_api.DriveWealthPortfolioApiProvider
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserEquityRepo
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserPortfolioRepo
import sdk.trade.repositories.repos.EquityTimeSeriesRepo
import sdk.trade.repositories.repos.PortfolioRepoIdentifier

class DriveWealthUserEquityRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var assetProvider: AssetProvider
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetRepo: AssetRepo
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthPortfolioApiProvider: DriveWealthPortfolioApiProvider
    private lateinit var driveWealthUserPortfolioRepo: DriveWealthUserPortfolioRepo
    private lateinit var equityTimeSeriesRepo: EquityTimeSeriesRepo<out GenericPortfolioApiProvider>
    private lateinit var driveWealthUserEquityRepo: DriveWealthUserEquityRepo
    private lateinit var stockDataApiProvider: StockDataApiProvider
    private lateinit var priceDataRepo: PriceDataRepo
    private lateinit var sessionsRepo: SessionsRepo
    private lateinit var sessionProvider: SessionProvider



    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        driveWealthHttpHandler = HTTPHandler(httpURL = "uat.drivewealth.finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetRepo = AssetRepo(MockStorage(),coreApiProvider)
        assetProvider = AssetProvider(assetRepo = assetRepo)
        driveWealthPortfolioApiProvider = DriveWealthPortfolioApiProvider(driveWealthHttpHandler, "api/v1/tr")
        stockDataApiProvider = StockDataApiProvider(baseHttpHandler)
        driveWealthUserPortfolioRepo = DriveWealthUserPortfolioRepo(
            MockStorage(),
            driveWealthPortfolioApiProvider,
            assetProvider,
        )
        sessionsRepo = SessionsRepo(
            MockStorage(),
            coreApiProvider,
        ) {
            "Europe/Istanbul"
        }
        sessionProvider = SessionProvider(sessionsRepo = sessionsRepo)
        priceDataRepo = PriceDataRepo(MockStorage(), stockDataApiProvider, sessionProvider, assetProvider)

        driveWealthUserEquityRepo = DriveWealthUserEquityRepo(
            MockStorage(),
            driveWealthPortfolioApiProvider,
            priceDataRepo,
            sessionProvider,
            assetProvider,
        )
        equityTimeSeriesRepo = EquityTimeSeriesRepo(
            sessionProvider,
            MockStorage(),
            driveWealthPortfolioApiProvider
        )
    }
    @Test
    fun fetchDataTest() = runBlocking {
        val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
        sessionProvider.init()
        assetProvider.init(setOf(Region.American))


        val userPortfolio = driveWealthUserPortfolioRepo.getData(null)
            ?: fail("Couldnt fetch user portfolio on user equity repo test")

        val userEquityData = driveWealthUserEquityRepo.fetchData(
            PortfolioRepoIdentifier(emptyList(), userPortfolio.portfolioAssets)
        )
        assertNotNull(userEquityData)
    }

    @Test
    fun fetchAfterTokenExpireScenario() = runBlocking {
        driveWealthHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
        val userEquityData = driveWealthUserEquityRepo.fetchData(
            PortfolioRepoIdentifier(emptyList(), emptyMap())
        )
        assertNull(userEquityData)

        val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
        sessionProvider.init()
        assetProvider.init(setOf(Region.American))

        val userPortfolio = driveWealthUserPortfolioRepo.getData(null)
            ?: fail("Couldnt fetch user portfolio on user equity repo test")

        val userEquityData2 = driveWealthUserEquityRepo.fetchData(
            PortfolioRepoIdentifier(emptyList(), userPortfolio.portfolioAssets)
        )
        assertNotNull(userEquityData2)
    }
    @Test
    fun equityGraphTests()  = runBlocking{
        // Update with a recent token before the test
        baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJkd19hY2NvdW50X2lkIjoiMmMwYjQxYTEtNDg0ZC00NDhjLThjNTUtOTNkNDIyYzFlMzhjLjE2OTI5NTY5NTI0MzYiLCJkd19hY2NvdW50X25vIjoiVUNDTTAwMDAwMSIsImV4cCI6MTY5OTA1ODQ3OCwianVyaXNkaWN0aW9uIjoidHIiLCJsb2NhbGUiOiJ0ciIsInJlYWQ6ZmlsdGVyX2RldGFpbCI6dHJ1ZSwicmVhZDpmaWx0ZXJfZGV0YWlsX3VzIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwicmVhZDpzZWN0b3JfdXMiOnRydWUsInVzZXJuYW1lIjoiS2VyZU0ifQ.f68mUl3-RsLW_Q6TxZdX30LME1HE6sszqIK07XG8f8KYdyzJb9hHAY2Ct-8W4BGu5qDV4zY34EMddtGf1BxL9Q"
        driveWealthHttpHandler.token = baseHttpHandler.token

        if (!sessionProvider.initialized) {
            runBlocking { sessionProvider.init() }
        }

        val timeSeries = equityTimeSeriesRepo.getData(StockDataPeriods.Price1W)
        assertNotNull(timeSeries)
        assertTrue(timeSeries!!.data.isNotEmpty())
        assertNotNull(timeSeries.currentPrice)
        assertTrue(timeSeries.currentPrice >= 0)
    }
}