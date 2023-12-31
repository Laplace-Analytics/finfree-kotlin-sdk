package repositories

import MockStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.logger
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.Asset
import sdk.models.data.assets.Region
import sdk.models.data.assets.string
import sdk.repositories.AssetRepo
import sdk.repositories.AssetsRepoIdentifier

class AssetRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetRepo: AssetRepo
    private var regionListWithoutTest = mutableListOf<Region>()



    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetRepo = AssetRepo(MockStorage(),coreApiProvider)

        for (region in Region.values()) {
            if (region != Region.Test) {
                regionListWithoutTest.add(region)
            }
        }
    }

    @Test
    fun `Basic usage test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        val assets: List<Asset>? = assetRepo.getData(
            AssetsRepoIdentifier(
                region =  Region.Turkish,
        )

        )
        assertNotNull(assets)

        val assets2: List<Asset>? = assetRepo.getData(
            AssetsRepoIdentifier(
                region =  Region.American,
        )

        )
        assertNotNull(assets2)
    }

    @Test
    fun `Fetch specific region test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        val assetTestRegionList = regionListWithoutTest

        for (region in assetTestRegionList) {
            val assets: List<Asset>? = assetRepo.getData(
                AssetsRepoIdentifier(
                    region =  region,
                )
            )
            if (assets == null || assets.isEmpty()) {
                logger.error("UNDEFINED OR ERROR REGION FOR GET ALL ASSETS: ${region.string()}")
                break
            }
            for (asset in assets) {
                assertEquals(region, asset.region)
                assertNotNull(asset.id)
            }
            delay(1000)
        }
    }

}