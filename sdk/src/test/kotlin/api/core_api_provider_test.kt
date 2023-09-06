package api

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.base.network.HTTPHandler
import sdk.models.AssetClass
import sdk.models.CollectionType
import sdk.models.Region
import sdk.models.string
import java.time.LocalDateTime
import java.time.ZoneId

class CoreApiProviderTest {

    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var regionListWithoutTest: List<Region>
    private lateinit var assetClassListWithoutForex: List<AssetClass>

    @BeforeEach
    fun setup(){
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        authApiProvider = AuthApiProvider(baseHttpHandler)

         regionListWithoutTest = Region.values().filter { it != Region.test }
         assetClassListWithoutForex = AssetClass.values().filter { it != AssetClass.forex }
    }

    @Nested
    inner class GetPredefinedCollectionsTest {
        @Test
        fun `Fetch without token state`() = runBlocking {
            val getPredefinedCollectionsResponse = coreApiProvider.getPredefinedCollections(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getPredefinedCollectionsResponse.responseType)
            assertNull(getPredefinedCollectionsResponse.data)
            assertEquals("Unauthorized\n", getPredefinedCollectionsResponse.message)

            val getPredefinedCollectionsResponse2 = coreApiProvider.getPredefinedCollections(Region.american)
            assertEquals(BasicResponseTypes.Error, getPredefinedCollectionsResponse2.responseType)
            assertNull(getPredefinedCollectionsResponse2.data)
            assertEquals("Unauthorized\n", getPredefinedCollectionsResponse2.message)
        }

        @Test
        fun `Fetch with expired token state`() {
            runBlocking {
                baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

                val getPredefinedCollectionsResponse = coreApiProvider.getPredefinedCollections(Region.turkish)
                assertEquals(BasicResponseTypes.Error, getPredefinedCollectionsResponse.responseType)
                assertNull(getPredefinedCollectionsResponse.data)
                assertEquals("Unauthorized\n", getPredefinedCollectionsResponse.message)


                val getPredefinedCollectionsResponse2 = coreApiProvider.getPredefinedCollections(Region.american)
                assertEquals(BasicResponseTypes.Error, getPredefinedCollectionsResponse2.responseType)
                assertNull(getPredefinedCollectionsResponse2.data)
                assertEquals("Unauthorized\n", getPredefinedCollectionsResponse2.message)
            }
        }
        @Test
        fun `Successful state`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getPredefinedCollectionsResponse = coreApiProvider.getPredefinedCollections(Region.turkish)
            assertEquals(BasicResponseTypes.Success, getPredefinedCollectionsResponse.responseType)
            assertNotNull(getPredefinedCollectionsResponse.data)

            val getPredefinedCollectionsResponse2 = coreApiProvider.getPredefinedCollections(Region.american)
            assertEquals(BasicResponseTypes.Success, getPredefinedCollectionsResponse2.responseType)
            assertNotNull(getPredefinedCollectionsResponse2.data)
        }

        @Test
        fun `Correct region stocks test`(): Unit = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getPredefinedCollectionsResponse = coreApiProvider.getPredefinedCollections(Region.turkish)
            assertEquals(BasicResponseTypes.Success, getPredefinedCollectionsResponse.responseType)
            assertNotNull(getPredefinedCollectionsResponse.data)
            getPredefinedCollectionsResponse.data?.forEach { collectionMap ->
                assertEquals(Region.turkish.string(), collectionMap["region"])
            }

            val getPredefinedCollectionsResponse2 = coreApiProvider.getPredefinedCollections(Region.american)
            assertEquals(BasicResponseTypes.Success, getPredefinedCollectionsResponse2.responseType)
            assertNotNull(getPredefinedCollectionsResponse2.data)
            getPredefinedCollectionsResponse2.data?.forEach { collectionMap ->
                assertEquals(Region.american.string(), collectionMap["region"])
            }
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getPredefinedCollectionsResponse = coreApiProvider.getPredefinedCollections(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getPredefinedCollectionsResponse.responseType)
            assertNull(getPredefinedCollectionsResponse.data)
            assertEquals("Unauthorized\n", getPredefinedCollectionsResponse.message)

            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getPredefinedCollectionsResponse2 = coreApiProvider.getPredefinedCollections(Region.american)
            assertEquals(BasicResponseTypes.Success, getPredefinedCollectionsResponse2.responseType)
            assertNotNull(getPredefinedCollectionsResponse2.data)
        }
    }

    @Nested
    inner class GetAllStocksTest(){
        @Test
        fun `Fetch with expired token state`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getAllStocksResponse = coreApiProvider.getAllStocks(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getAllStocksResponse.responseType)
            assertNull(getAllStocksResponse.data)
            assertEquals("Unauthorized\n", getAllStocksResponse.message)

            val getAllStocksResponse2 = coreApiProvider.getAllStocks(Region.american)
            assertEquals(BasicResponseTypes.Error, getAllStocksResponse2.responseType)
            assertNull(getAllStocksResponse2.data)
            assertEquals("Unauthorized\n", getAllStocksResponse2.message)
        }

        @Test
        fun `Fetch without token state`() = runBlocking {
            val getAllStocksResponse = coreApiProvider.getAllStocks(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getAllStocksResponse.responseType)
            assertNull(getAllStocksResponse.data)
            assertEquals("Unauthorized\n", getAllStocksResponse.message)

            val getAllStocksResponse2 = coreApiProvider.getAllStocks(Region.american)
            assertEquals(BasicResponseTypes.Error, getAllStocksResponse2.responseType)
            assertNull(getAllStocksResponse2.data)
            assertEquals("Unauthorized\n", getAllStocksResponse2.message)
        }

        @Test
        fun `Successful state`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getAllStocksResponse = coreApiProvider.getAllStocks(Region.turkish)
            assertEquals(BasicResponseTypes.Success, getAllStocksResponse.responseType)
            assertNotNull(getAllStocksResponse.data)
            assertTrue((getAllStocksResponse.data?.size ?: 0) > 0)

            val getAllStocksResponse2 = coreApiProvider.getAllStocks(Region.american)
            assertEquals(BasicResponseTypes.Success, getAllStocksResponse2.responseType)
            assertNotNull(getAllStocksResponse2.data)
            assertTrue((getAllStocksResponse2.data?.size ?: 0) > 0)
        }
        //
        @Test
        fun `Request with secondsSinceEpoch test`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val secondsSinceEpoch = LocalDateTime.now().minusDays(1).second.toLong()

            println(secondsSinceEpoch)

            val getAllStocksResponse = coreApiProvider.getAllStocks(
                Region.turkish,
                secondsSinceEpoch = secondsSinceEpoch
            )
            assertEquals(BasicResponseTypes.Success, getAllStocksResponse.responseType)
            assertNotNull(getAllStocksResponse.data)
            assertTrue((getAllStocksResponse.data?.size ?: 0) > 0)

            val getAllStocksResponse2 = coreApiProvider.getAllStocks(
                Region.american,
                secondsSinceEpoch = secondsSinceEpoch
            )
            assertEquals(BasicResponseTypes.Success, getAllStocksResponse2.responseType)
            assertNotNull(getAllStocksResponse2.data)
            assertTrue((getAllStocksResponse2.data?.size ?: 0) > 0)
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getAllStocksResponse = coreApiProvider.getAllStocks(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getAllStocksResponse.responseType)
            assertNull(getAllStocksResponse.data)
            assertEquals("Unauthorized\n", getAllStocksResponse.message)

            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getAllStocksResponse2 = coreApiProvider.getAllStocks(Region.american)
            assertEquals(BasicResponseTypes.Success, getAllStocksResponse2.responseType)
            assertNotNull(getAllStocksResponse2.data)
            assertTrue((getAllStocksResponse2.data?.size ?: 0) > 0)
        }
    }

    @Nested
    inner class GetSessionsTests{
        @Test
        fun `Fetch with expired token state`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getSessionsResponse = coreApiProvider.getSessions()
            assertEquals(BasicResponseTypes.Error, getSessionsResponse.responseType)
            assertNull(getSessionsResponse.data)
            assertEquals("Unauthorized\n", getSessionsResponse.message)
        }

        @Test
        fun `Fetch without token state`() = runBlocking {
            val getSessionsResponse = coreApiProvider.getSessions()
            assertEquals(BasicResponseTypes.Error, getSessionsResponse.responseType)
            assertNull(getSessionsResponse.data)
            assertEquals("Unauthorized\n", getSessionsResponse.message)
        }

        @Test
        fun `Fetch specific region test`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            for (region in regionListWithoutTest) {
                val getSessionsResponse = coreApiProvider.getSessions(region = region)
                assertEquals(BasicResponseTypes.Success, getSessionsResponse.responseType)
                assertNotNull(getSessionsResponse.data)
                assertTrue((getSessionsResponse.data?.size ?: 0) > 0)

                for (session:Map<String,Any> in getSessionsResponse.data as List<Map<String,Any>>){
                    assertEquals(region.string(), session["region"])
                }
            }
        }

        @Test
        fun `Fetch specific assetClass test`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            for (assetClass in assetClassListWithoutForex) {
                val getSessionsResponse = coreApiProvider.getSessions(assetClass = assetClass)
                assertEquals(BasicResponseTypes.Success, getSessionsResponse.responseType)
                assertNotNull(getSessionsResponse.data)
                assertTrue((getSessionsResponse.data?.size ?: 0) > 0)

                for (session:Map<String,Any> in getSessionsResponse.data as List<Map<String,Any>>){
                    assertEquals(assetClass.string(), session["asset_class"])
                }
            }
        }
        @Test
        fun `Fetch all region and assetClass test`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getSessionsResponse = coreApiProvider.getSessions()
            assertEquals(BasicResponseTypes.Success, getSessionsResponse.responseType)
            assertNotNull(getSessionsResponse.data)
            assertTrue((getSessionsResponse.data?.size ?: 0) > 0)

            var isThereTurkishRegion = false
            var isThereAmericanRegion = false
            var isThereEquityClass = false
            var isThereCryptoClass = false

            (getSessionsResponse.data as List<Map<String,Any>>).forEach { session:Map<String,Any> ->
                if (!isThereTurkishRegion && session["region"] == Region.turkish.string()) {
                    isThereTurkishRegion = true
                }
                if (!isThereAmericanRegion && session["region"] == Region.american.string()) {
                    isThereAmericanRegion = true
                }
                if (!isThereEquityClass && session["asset_class"] == AssetClass.equity.string()) {
                    isThereEquityClass = true
                }
                if (!isThereCryptoClass && session["asset_class"] == AssetClass.crypto.string()) {
                    isThereCryptoClass = true
                }
            }

            assertTrue(isThereTurkishRegion)
            assertTrue(isThereAmericanRegion)
            assertTrue(isThereEquityClass)
            assertTrue(isThereCryptoClass)
        }

        @Test
        fun `Fetch specific region and assetClass test`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val sessionTestRegion = regionListWithoutTest.toList()
            val sessionTestAssetClass = AssetClass.values().toList()
            val undefinedSessionGroups = mutableListOf<Map<String, String>>()

            for (region in sessionTestRegion) {
                for (assetClass in sessionTestAssetClass) {
                    val getCollectionsResponse = coreApiProvider.getSessions(
                        region = region,
                        assetClass = assetClass
                    )
                    if (getCollectionsResponse.responseType == BasicResponseTypes.Error) {
                        undefinedSessionGroups.add(
                            mapOf(
                                "region" to region.string(),
                                "asset_class" to assetClass.string()
                            )
                        )
                        break
                    }
                    assertEquals(BasicResponseTypes.Success, getCollectionsResponse.responseType)
                    assertNotNull(getCollectionsResponse.data)
                    assertTrue(getCollectionsResponse.data?.size ?: 0 > 0)
                    delay(1000) // 1 second delay
                }
            }
            logger.error("UNDEFINED SESSION GROUPS $undefinedSessionGroups")
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getSessionsResponse = coreApiProvider.getSessions()
            assertEquals(BasicResponseTypes.Error, getSessionsResponse.responseType)
            assertNull(getSessionsResponse.data)
            assertEquals("Unauthorized\n", getSessionsResponse.message)

            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getSessionsResponse2 = coreApiProvider.getSessions()
            assertEquals(BasicResponseTypes.Success, getSessionsResponse2.responseType)
            assertNotNull(getSessionsResponse2.data)
            assertTrue((getSessionsResponse2.data?.size ?: 0) > 0)
        }
    }

    @Nested
    inner class GetCollectionTest{
        @Test
        fun `Fetch with expired token state`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getCollectionsResponse = coreApiProvider.getCollections(Region.american, CollectionType.collection)
            assertEquals(BasicResponseTypes.Error, getCollectionsResponse.responseType)
            assertNull(getCollectionsResponse.data)
            assertEquals("Unauthorized\n", getCollectionsResponse.message)
        }

        @Test
        fun `Fetch without token state`() = runBlocking {
            val getSessionsResponse = coreApiProvider.getCollections(Region.turkish, CollectionType.industry)
            assertEquals(BasicResponseTypes.Error, getSessionsResponse.responseType)
            assertNull(getSessionsResponse.data)
            assertEquals("Unauthorized\n", getSessionsResponse.message)
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
            val collectionTestCollectionType = CollectionType.values()
            val undefinedCollectionGroups = mutableListOf<String>()

            for (region in collectionTestRegion) {
                for (collectionType in collectionTestCollectionType) {
                    val getCollectionsResponse = coreApiProvider.getCollections(region, collectionType)
                    if (getCollectionsResponse.responseType == BasicResponseTypes.Error) {
                        undefinedCollectionGroups.add("stock/${collectionType.string()}/${region.string()}'")
                        break
                    }
                    assertEquals(BasicResponseTypes.Success, getCollectionsResponse.responseType)
                    assertNotNull(getCollectionsResponse.data)
                    assertTrue(getCollectionsResponse.data?.size ?: 0 > 0)
                    delay(1000)
                }
            }
            logger.error("UNDEFINED COLLECTION GROUPS $undefinedCollectionGroups")
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getSessionsResponse = coreApiProvider.getCollections(Region.turkish, CollectionType.sector)
            assertEquals(BasicResponseTypes.Error, getSessionsResponse.responseType)
            assertNull(getSessionsResponse.data)
            assertEquals("Unauthorized\n", getSessionsResponse.message)

            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getCollectionsResponse2 = coreApiProvider.getCollections(Region.turkish, CollectionType.sector)
            assertEquals(BasicResponseTypes.Success, getCollectionsResponse2.responseType)
            assertNotNull(getCollectionsResponse2.data)
            assertTrue(getCollectionsResponse2.data?.size ?: 0 > 0)
        }
    }
    @Nested
    inner class GetJurisdictionTest{
        @Test
        fun `Fetch with expired token state`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val getJurisdictionResponse = coreApiProvider.getJurisdiction(Region.american)
            assertEquals(BasicResponseTypes.Error, getJurisdictionResponse.responseType)
            assertNull(getJurisdictionResponse.data)
            assertEquals("Unauthorized\n", getJurisdictionResponse.message)
        }

        @Test
        fun `Fetch without token state`() = runBlocking {
            val getJurisdictionResponse = coreApiProvider.getJurisdiction(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getJurisdictionResponse.responseType)
            assertNull(getJurisdictionResponse.data)
            assertEquals("Unauthorized\n", getJurisdictionResponse.message)
        }

        @Test
        fun `Fetch specific region test`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            for (region in regionListWithoutTest) {
                val getJurisdictionResponse = coreApiProvider.getJurisdiction(region)
                assertEquals(BasicResponseTypes.Success, getJurisdictionResponse.responseType)
                assertNotNull(getJurisdictionResponse.data)
                assertEquals(region.string(), getJurisdictionResponse.data?.get("code"))
                delay(1000L)  // 1 second delay
            }
        }

        @Test
        fun `Fetch after token expire scenario`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            var getJurisdictionResponse = coreApiProvider.getJurisdiction(Region.turkish)
            assertEquals(BasicResponseTypes.Error, getJurisdictionResponse.responseType)
            assertNull(getJurisdictionResponse.data)
            assertEquals("Unauthorized\n", getJurisdictionResponse.message)

            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            getJurisdictionResponse = coreApiProvider.getJurisdiction(Region.turkish)
            assertEquals(BasicResponseTypes.Success, getJurisdictionResponse.responseType)
            assertNotNull(getJurisdictionResponse.data)
        }
    }

    @Nested
    inner class PostJurisdictionTest{
        @Test
        fun `Post with expired token state`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val postJurisdictionResponse = coreApiProvider.postJurisdiction(
                jurisdiction = Region.american,
                locale = Region.american
            )
            assertEquals(BasicResponseTypes.Error, postJurisdictionResponse.responseType)
            assertEquals("Unauthorized\n", postJurisdictionResponse.message)
        }

        @Test
        fun `Post without token state`() = runBlocking {
            val postJurisdictionResponse = coreApiProvider.postJurisdiction(
                jurisdiction = Region.turkish,
                locale = Region.american
            )
            assertEquals(BasicResponseTypes.Error, postJurisdictionResponse.responseType)
            assertEquals("Unauthorized\n", postJurisdictionResponse.message)
        }
        @Test
        fun `Post specific jurisdiction and locale`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            for (jurisdiction in regionListWithoutTest) {
                for (locale in regionListWithoutTest) {
                    val postJurisdictionResponse = coreApiProvider.postJurisdiction(
                        jurisdiction = jurisdiction,
                        locale = locale
                    )
                    assertEquals(BasicResponseTypes.Success, postJurisdictionResponse.responseType)
                    delay(1000L)
                }
            }
        }

        @Test
        fun `Post after token expire scenario`() = runBlocking {
            baseHttpHandler.token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb250ZW50X2NyZWF0b3IiOnRydWUsImR3X2FjY291bnRfaWQiOiI4Yzg5OGEyNi05YTU0LTQxMTktOTBiMy0xNTI0NjhjYjU0ZGUuMTY4MTIxMzQ1MTk2MSIsImR3X2FjY291bnRfbm8iOiJGRkFZMDAwMDAxIiwiZXhwIjoxNjkwMzIzNTY3LCJqdXJpc2RpY3Rpb24iOiJ0ciIsImxvY2FsZSI6InRyIiwicmVhZDpmaWx0ZXJfZGV0YWlsIjp0cnVlLCJyZWFkOnJ0X3ByaWNlIjp0cnVlLCJyZWFkOnNlY3RvciI6dHJ1ZSwidXNlcm5hbWUiOiJjbnRya3kifQ.XMOIoR1WdsIUQ9qqy5s31atLv1DfSLeCrijIUNbqrAXCidJI7T39lNM7dGODgofb9gzs9MOfLJr5eateUGHaKw"

            val postJurisdictionResponse = coreApiProvider.postJurisdiction(
                jurisdiction = Region.turkish,
                locale = Region.turkish
            )
            assertEquals(BasicResponseTypes.Error, postJurisdictionResponse.responseType)
            assertEquals("Unauthorized\n", postJurisdictionResponse.message)

            val reLoginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (reLoginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val reLoginData = reLoginResponse.data!!
            baseHttpHandler.token = reLoginData.accessToken

            val postJurisdictionResponse2 = coreApiProvider.postJurisdiction(
                jurisdiction = Region.turkish,
                locale = Region.turkish
            )
            assertEquals(BasicResponseTypes.Success, postJurisdictionResponse2.responseType)
        }
    }

}