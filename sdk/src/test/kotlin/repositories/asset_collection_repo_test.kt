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
import sdk.models.data.assets.CollectionType
import sdk.models.data.assets.Region
import sdk.models.data.assets.string
import sdk.repositories.AssetCollectionRepo
import sdk.repositories.AssetCollectionRepoIdentifier

class AssetCollectionRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetCollectionRepo: AssetCollectionRepo
    private var regionListWithoutTest = mutableListOf<Region>()



    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetCollectionRepo = AssetCollectionRepo(MockStorage(),coreApiProvider)

        for (region in Region.values()) {
            if (region != Region.Test) {
                regionListWithoutTest.add(region)
            }
        }
    }
    @Test
    fun `Basic usage tests`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        val collections = assetCollectionRepo.fetchData(null)
        assertEquals(null, collections)

        val collections2 = assetCollectionRepo.fetchData(
            AssetCollectionRepoIdentifier(Region.Turkish, CollectionType.Sector)
        )
        assertNotNull(collections2)
    }

    @Test
    fun `Fetch specific region and collectionType test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        val collectionTestRegion = regionListWithoutTest
        val collectionTestCollectionType = CollectionType.values().toList()

        for (region in collectionTestRegion) {
            for (collectionType in collectionTestCollectionType) {
                val collections = assetCollectionRepo.fetchData(
                    AssetCollectionRepoIdentifier(region, collectionType)
                )
                if (collections == null || collections.isEmpty()) {
                    logger.error("UNDEFINED COLLECTION GROUP: ${region.string()}:${collectionType.string()}")
                    break
                }
                for (assetCollection in collections) {
                    assertEquals(collectionType, assetCollection.type)
                    assertNotNull(assetCollection.id)
                }
                delay(1000L)
            }
        }
    }

}