package trade.repositories

import MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.network.HTTPHandler
import sdk.models.Region
import sdk.models.core.AssetProvider
import sdk.repositories.AssetCollectionRepo
import sdk.repositories.AssetRepo
import sdk.trade.api.drivewealth_api.DriveWealthOrderAPIProvider
import sdk.trade.OrderData
import sdk.trade.repositories.drivewealth_repos.DriveWealthOrdersRepository

class DriveWealthOrdersRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var assetProvider: AssetProvider
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetRepo: AssetRepo
    private lateinit var assetCollectionRepo: AssetCollectionRepo
    private lateinit var driveWealthHttpHandler: HTTPHandler
    private lateinit var driveWealthOrdersRepo: DriveWealthOrdersRepository
    private lateinit var driveWealthOrderAPIProvider: DriveWealthOrderAPIProvider

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        driveWealthHttpHandler = HTTPHandler(httpURL = "drivewealth.finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetRepo = AssetRepo(MockStorage(),coreApiProvider)
        assetCollectionRepo = AssetCollectionRepo(MockStorage(),coreApiProvider)
        assetProvider = AssetProvider(assetRepo = assetRepo, assetCollectionRepo = assetCollectionRepo)
        driveWealthOrderAPIProvider = DriveWealthOrderAPIProvider(driveWealthHttpHandler, "api/v1/tr/order")
        driveWealthOrdersRepo = DriveWealthOrdersRepository(MockStorage(), driveWealthOrderAPIProvider, assetProvider)
    }

    @Test
    fun `Fetch data test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
        assetProvider.init(setOf(Region.american))

        val orderData: List<OrderData>? = driveWealthOrdersRepo.fetchData(null)
        assertNotNull(orderData)
    }

    @Test
    fun `Fetch after token expire scenario`() = runBlocking {
        driveWealthHttpHandler.token =
            "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw";
        val orderData: List<OrderData>? = driveWealthOrdersRepo.fetchData(null)
        assertNull(orderData)

        val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.token = loginData.accessToken
        driveWealthHttpHandler.constantHeaders["GEDIK-ACCOUNT-ID"] = "928607"
        assetProvider.init(setOf(Region.american))

        val orderData2: List<OrderData>? = driveWealthOrdersRepo.fetchData(null)
        assertNotNull(orderData2)
    }
}