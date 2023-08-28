package trade.repositories

import MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponse
import sdk.api.LoginResponseData
import sdk.api.LoginResponseTypes
import sdk.api.StockDataApiProvider
import sdk.base.network.HTTPHandler
import sdk.models.Region
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.AssetCollectionRepo
import sdk.repositories.AssetRepo
import sdk.repositories.PriceDataRepo
import sdk.repositories.SessionsRepo
import sdk.trade.generic_api.DriveWealthPortfolioApiProvider
import sdk.trade.models.portfolio.UserPortfolio
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserEquityRepo
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserPortfolioRepo

class DriveWealthUserPortfolioRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var assetProvider: AssetProvider
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetRepo: AssetRepo
    private lateinit var assetCollectionRepo: AssetCollectionRepo
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthPortfolioApiProvider: DriveWealthPortfolioApiProvider
    private lateinit var driveWealthUserPortfolioRepo: DriveWealthUserPortfolioRepo


    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        driveWealthHttpHandler = HTTPHandler(httpURL = "uat.drivewealth.finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetRepo = AssetRepo(MockStorage(),coreApiProvider)
        assetCollectionRepo = AssetCollectionRepo(MockStorage(),coreApiProvider)
        assetProvider = AssetProvider(assetRepo = assetRepo, assetCollectionRepo = assetCollectionRepo)
        driveWealthPortfolioApiProvider = DriveWealthPortfolioApiProvider(driveWealthHttpHandler, "api/v1/tr")
        driveWealthUserPortfolioRepo = DriveWealthUserPortfolioRepo(
            MockStorage(),
            driveWealthPortfolioApiProvider,
            assetProvider,
        )
    }
    @Nested
    inner class GetPortfolioTests{
        @Test
        fun `Fetch data test`() = runBlocking {
            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
            assetProvider.init(setOf(Region.american, Region.turkish))

            val userPortfolio: UserPortfolio? = driveWealthUserPortfolioRepo.fetchData(null)
            assertNotNull(userPortfolio)
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            driveWealthHttpHandler.token =
                "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"
            var userPortfolio: UserPortfolio? = driveWealthUserPortfolioRepo.fetchData(null)
            assertNull(userPortfolio)

            val loginResponse: LoginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData: LoginResponseData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.token = loginData.accessToken
            driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
            assetProvider.init(setOf(Region.american, Region.turkish))

            val userPortfolio2: UserPortfolio? = driveWealthUserPortfolioRepo.fetchData(null)
            assertNotNull(userPortfolio2)
        }

    }
}