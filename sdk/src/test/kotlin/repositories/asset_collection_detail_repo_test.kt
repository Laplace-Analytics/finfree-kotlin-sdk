package repositories

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.MockStorage
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.AssetCollection
import sdk.models.data.assets.CollectionType
import sdk.models.data.assets.Region
import sdk.repositories.AssetCollectionDetailRepo
import sdk.repositories.AssetCollectionDetailRepoIdentifier
import sdk.repositories.AssetCollectionRepo
import sdk.repositories.AssetCollectionRepoIdentifier

class AssetCollectionRepoTest {
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var assetCollectionRepo: AssetCollectionRepo
    private lateinit var assetCollectionDetailRepo: AssetCollectionDetailRepo

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        authApiProvider = AuthApiProvider(baseHttpHandler)
        assetCollectionRepo = AssetCollectionRepo(MockStorage(), coreApiProvider)
        assetCollectionDetailRepo = AssetCollectionDetailRepo(MockStorage(), coreApiProvider)
    }

    @Test
    fun basicUsageTests() = runBlocking {

        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!

        baseHttpHandler.token = loginData.accessToken

        val collections = assetCollectionRepo.fetchData(
            AssetCollectionRepoIdentifier(
                Region.Turkish,
                CollectionType.Collection
            )
        )
        if (collections.isNullOrEmpty()) {
            fail("Fetching collections in AssetCollectionDetailRepo Test failed ");
        }

        val testCollection: AssetCollection = collections.first()

        val assetCollectionDetail = assetCollectionDetailRepo.getData(
            AssetCollectionDetailRepoIdentifier(
                testCollection.id,
                testCollection.type
            )
        )
        assertNotNull(assetCollectionDetail)
        assertNotNull(assetCollectionDetail?.id)
    }
}
