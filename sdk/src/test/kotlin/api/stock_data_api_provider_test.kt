package api

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.LoginResponseTypes
import sdk.api.StockDataApiProvider
import sdk.api.StockDataPeriods
import sdk.api.StockStatistics
import sdk.base.network.BasicResponseTypes
import sdk.base.network.HTTPHandler
import sdk.models.AssetClass
import sdk.models.Region

class StockDataApiProviderTests{
    private lateinit var stockDataApiProvider: StockDataApiProvider
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler

    @BeforeEach
    fun setup(){
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        stockDataApiProvider = StockDataApiProvider(baseHttpHandler,"stock")
        authApiProvider = AuthApiProvider(baseHttpHandler)
    }

    @Nested
    inner class GetStockPriceDataTest{
        @Test
        fun `Successful state`() = runBlocking {
            val getStockDataResponse = stockDataApiProvider.getStockPriceData(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockDataPeriods.Price1D)
            )
            assertEquals(BasicResponseTypes.Success, getStockDataResponse.responseType)
            assertTrue(getStockDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1D.period) } ?: false)

            val getStockDataResponse2 = stockDataApiProvider.getStockPriceData(
                locale = Region.american,
                assetClass = AssetClass.equity,
                symbols = listOf("AAPL"),
                fields = listOf(StockDataPeriods.Price1W)
            )
            assertEquals(BasicResponseTypes.Success, getStockDataResponse2.responseType)
            assertTrue(getStockDataResponse2.data?.any { it.containsKey(StockDataPeriods.Price1W.period) } ?: false)
        }

        @Test
        fun `Multiple period state`() = runBlocking {
            val getStockDataResponse = stockDataApiProvider.getStockPriceData(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockDataPeriods.Price1D, StockDataPeriods.Price1M, StockDataPeriods.Price1Y, StockDataPeriods.Price5Y)
            )
            assertEquals(BasicResponseTypes.Success, getStockDataResponse.responseType)
            assertNotNull(getStockDataResponse.data)
            assertTrue(getStockDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1D.period) } ?: false)
            assertTrue(getStockDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1M.period) } ?: false)
            assertTrue(getStockDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1Y.period) } ?: false)
            assertTrue(getStockDataResponse.data?.any { it.containsKey(StockDataPeriods.Price5Y.period) } ?: false)
        }

        @Test
        fun `Wrong region state`() = runBlocking {
            val getStockDataResponse = stockDataApiProvider.getStockPriceData(
                locale = Region.american,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockDataPeriods.Price1D)
            )
            assertEquals(BasicResponseTypes.Error, getStockDataResponse.responseType)
            assertNull(getStockDataResponse.data)
        }

        @Test
        fun `Wrong symbol state`() = runBlocking {
            val getStockPriceDataResponse = stockDataApiProvider.getStockPriceData(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THY"),
                fields = listOf(StockDataPeriods.Price1D)
            )
            assertEquals(BasicResponseTypes.Error, getStockPriceDataResponse.responseType)
            assertNull(getStockPriceDataResponse.data)
        }

        @Test
        fun `Duplicate period state`() = runBlocking {
            val getStockPriceDataResponse = stockDataApiProvider.getStockPriceData(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockDataPeriods.Price1D, StockDataPeriods.Price1D, StockDataPeriods.Price1M, StockDataPeriods.Price1M)
            )
            assertEquals(BasicResponseTypes.Success, getStockPriceDataResponse.responseType)
            assertNotNull(getStockPriceDataResponse.data)
            assertTrue(getStockPriceDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1D.period) } == true)
            assertTrue(getStockPriceDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1M.period) } == true)
        }
        @Test
        fun `Request with bearer token`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            val getStockPriceDataResponse = stockDataApiProvider.getStockPriceData(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockDataPeriods.Price1D)
            )
            assertEquals(BasicResponseTypes.Success, getStockPriceDataResponse.responseType)
            assertNotNull(getStockPriceDataResponse.data)
            assertTrue(getStockPriceDataResponse.data?.any { it.containsKey(StockDataPeriods.Price1D.period) } == true)
        }
    }
    @Nested
    inner class GetCryptoStatisticsTest{
        @Test
        fun `Successful state`() = runBlocking {
            baseHttpHandler.token = null
            val getCryptoStatisticsResponse = stockDataApiProvider.getCryptoStatistics(symbol = "ETHUSD")
            assertEquals(BasicResponseTypes.Success, getCryptoStatisticsResponse.responseType)
            assertNotNull(getCryptoStatisticsResponse.data)
        }

        @Test
        fun `Wrong symbol state`() = runBlocking {
            baseHttpHandler.token = null
            val getCryptoStatisticsResponse = stockDataApiProvider.getCryptoStatistics(symbol = "THYAO")
            assertEquals(BasicResponseTypes.Error, getCryptoStatisticsResponse.responseType)
            assertNull(getCryptoStatisticsResponse.data)

            val getCryptoStatisticsResponse2 = stockDataApiProvider.getCryptoStatistics(symbol = "ETHHUSD")
            assertEquals(BasicResponseTypes.Error, getCryptoStatisticsResponse2.responseType)
            assertNull(getCryptoStatisticsResponse2.data)
        }

        @Test
        fun `Request with bearer token`() = runBlocking {
            baseHttpHandler.token = null
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            val getCryptoStatisticsResponse = stockDataApiProvider.getCryptoStatistics(symbol = "BTCTRY")
            assertEquals(BasicResponseTypes.Success, getCryptoStatisticsResponse.responseType)
            assertNotNull(getCryptoStatisticsResponse.data)
        }

    }
    @Nested
    inner class GetStockStatisticsTest{
        @Test
        fun `Successful state`() = runBlocking {
            val getStockStatisticsResponse = stockDataApiProvider.getStockStatistics(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockStatistics.pbRatio)
            )

            assertEquals(BasicResponseTypes.Success, getStockStatisticsResponse.responseType)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.pbRatio.slug) } == true)

            for (stat in StockStatistics.values()) {
                if (stat == StockStatistics.pbRatio) {
                    break
                }
                assertFalse(getStockStatisticsResponse.data?.any { it.containsKey(stat.slug) } == true)
            }

            val getStockStatisticsResponse2 = stockDataApiProvider.getStockStatistics(
                locale = Region.american,
                assetClass = AssetClass.equity,
                symbols = listOf("AAPL"),
                fields = listOf(StockStatistics.peRatio)
            )

            assertEquals(BasicResponseTypes.Success, getStockStatisticsResponse2.responseType)
            assertTrue(getStockStatisticsResponse2.data?.any { it.containsKey(StockStatistics.peRatio.slug) } == true)

            for (stat in StockStatistics.values()) {
                if (stat == StockStatistics.peRatio) {
                    break
                }
                assertFalse(getStockStatisticsResponse.data?.any { it.containsKey(stat.slug) } == true)
            }
        }
        @Test
        fun `Multiple statistics state`() = runBlocking {
            val getStockStatisticsResponse = stockDataApiProvider.getStockStatistics(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockStatistics.peRatio, StockStatistics.totalShares, StockStatistics.yearLow, StockStatistics.yearHigh)
            )

            assertEquals(BasicResponseTypes.Success, getStockStatisticsResponse.responseType)
            assertNotNull(getStockStatisticsResponse.data)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.peRatio.slug) } == true)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.totalShares.slug) } == true)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.yearLow.slug) } == true)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.yearHigh.slug) } == true)
        }

        @Test
        fun `Wrong region state`() = runBlocking {
            val getStockStatisticsResponse = stockDataApiProvider.getStockStatistics(
                locale = Region.american,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockStatistics.totalShares)
            )

            assertEquals(BasicResponseTypes.Error, getStockStatisticsResponse.responseType)
            assertNull(getStockStatisticsResponse.data)
        }

        @Test
        fun `Wrong symbol state`() = runBlocking {
            val getStockStatisticsResponse = stockDataApiProvider.getStockStatistics(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THY"),
                fields = listOf(StockStatistics.totalShares)
            )

            assertEquals(BasicResponseTypes.Error, getStockStatisticsResponse.responseType)
            assertNull(getStockStatisticsResponse.data)
        }

        @Test
        fun `Duplicate period state`() = runBlocking {
            val getStockStatisticsResponse = stockDataApiProvider.getStockStatistics(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockStatistics.peRatio, StockStatistics.yearHigh, StockStatistics.peRatio, StockStatistics.yearHigh)
            )

            assertEquals(BasicResponseTypes.Success, getStockStatisticsResponse.responseType)
            assertNotNull(getStockStatisticsResponse.data)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.peRatio.slug) } == true)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.yearHigh.slug) } == true)
        }

        @Test
        fun `Request with bearer token`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")

            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }

            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken

            val getStockStatisticsResponse = stockDataApiProvider.getStockStatistics(
                locale = Region.turkish,
                assetClass = AssetClass.equity,
                symbols = listOf("THYAO"),
                fields = listOf(StockStatistics.peRatio)
            )

            assertEquals(BasicResponseTypes.Success, getStockStatisticsResponse.responseType)
            assertNotNull(getStockStatisticsResponse.data)
            assertTrue(getStockStatisticsResponse.data?.any { it.containsKey(StockStatistics.peRatio.slug) } == true)
        }
    }

    @Nested
    inner class GetAggregatedPriceDataTest {
        @Test
        fun `Successful state`() = runBlocking {
            baseHttpHandler.token = null
            val getAggregatedPriceDataResponse = stockDataApiProvider.getAggregatedPriceData(
                symbols = listOf("THYAO"),
                period = StockDataPeriods.Price1D,
                region = Region.turkish,
                assetClass = AssetClass.equity
            )

            assertEquals(BasicResponseTypes.Success, getAggregatedPriceDataResponse.responseType)
            assertNotNull(getAggregatedPriceDataResponse.data)
            assertTrue((getAggregatedPriceDataResponse.data!!["graph"] as List<*>).isNotEmpty())

            baseHttpHandler.token = null
            val getAggregatedPriceDataResponse2 = stockDataApiProvider.getAggregatedPriceData(
                symbols = listOf("AAPL"),
                period = StockDataPeriods.Price1D,
                region = Region.american,
                assetClass = AssetClass.equity
            )

            assertEquals(BasicResponseTypes.Success, getAggregatedPriceDataResponse2.responseType)
            assertNotNull(getAggregatedPriceDataResponse2.data)
            assertTrue((getAggregatedPriceDataResponse2.data!!["graph"] as List<*>).isNotEmpty())
        }

        @Test
        fun `Wrong region state`() = runBlocking {
            baseHttpHandler.token = null
            val getAggregatedPriceDataResponse = stockDataApiProvider.getAggregatedPriceData(
                symbols = listOf("THYAO"),
                period = StockDataPeriods.Price1D,
                region = Region.american,
                assetClass = AssetClass.equity
            )
            assertEquals(BasicResponseTypes.Error, getAggregatedPriceDataResponse.responseType)
            assertNull(getAggregatedPriceDataResponse.data)
        }

        @Test
        fun `Wrong symbol state`() = runBlocking {
            baseHttpHandler.token = null
            val getAggregatedPriceDataResponse = stockDataApiProvider.getAggregatedPriceData(
                symbols = listOf("THY"),
                period = StockDataPeriods.Price1D,
                region = Region.turkish,
                assetClass = AssetClass.equity
            )
            assertEquals(BasicResponseTypes.Error, getAggregatedPriceDataResponse.responseType)
            assertNull(getAggregatedPriceDataResponse.data)
        }

        @Test
        fun `Request with bearer token`() = runBlocking {
            val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
            if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
                fail("Could not login to Finfree Account")
            }
            val loginData = loginResponse.data!!
            baseHttpHandler.token = loginData.accessToken
            val getAggregatedPriceDataResponse = stockDataApiProvider.getAggregatedPriceData(
                symbols = listOf("SISE"),
                period = StockDataPeriods.Price1D,
                region = Region.turkish,
                assetClass = AssetClass.equity
            )
            assertEquals(BasicResponseTypes.Success, getAggregatedPriceDataResponse.responseType)
            assertNotNull(getAggregatedPriceDataResponse.data)
            assertTrue((getAggregatedPriceDataResponse.data!!["graph"] as List<*>).isNotEmpty())
        }
    }

}