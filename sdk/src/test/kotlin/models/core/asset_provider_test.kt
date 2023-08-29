package models.core

import MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

class AssetProviderTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var assetProvider: AssetProvider
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetRepo: AssetRepo
    private lateinit var assetCollectionRepo: AssetCollectionRepo

    @BeforeEach
    fun setup() {
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetRepo = AssetRepo(MockStorage(),coreApiProvider)
        assetCollectionRepo = AssetCollectionRepo(MockStorage(),coreApiProvider)
        assetProvider = AssetProvider(assetRepo = assetRepo, assetCollectionRepo = assetCollectionRepo)
    }
    @Test
    fun `Basic usage tests`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")

        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }

        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        assetProvider.init(setOf(Region.american, Region.turkish))

        assertTrue(assetProvider.allAssets.isNotEmpty())
        assertEquals(true, assetProvider.initialized)
        assertTrue(assetProvider.allSectors.isNotEmpty())
    }

}