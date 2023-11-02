package repositories

import MockStorage
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseTypes
import sdk.api.StockDataApiProvider
import sdk.api.StockDataPeriods
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.Asset
import sdk.models.data.assets.AssetType
import sdk.models.data.assets.Region
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.AggregatedPriceDataSeriesIdentifier
import sdk.repositories.AggregatedPriceDataSeriesRepo
import sdk.repositories.AssetRepo
import sdk.repositories.PriceDataRepo
import sdk.repositories.SessionsRepo

class AggregatedPriceDataSeriesRepoTest {
    private lateinit var stockDataApiProvider: StockDataApiProvider
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var sessionProvider: SessionProvider
    private lateinit var assetProvider: AssetProvider
    private lateinit var assetRepo: AssetRepo
    private lateinit var priceDataRepo: PriceDataRepo
    private lateinit var sessionsRepo: SessionsRepo
    private lateinit var aggregatedPriceDataSeriesRepo: AggregatedPriceDataSeriesRepo
    private var testAssetsJustHaveSymbol = listOf(
        Asset(
            id = "id",
            name = "name",
            symbol = "THYAO",
            industryId = "industryId",
            sectorId = "sectorId",
            isActive = true,
            region = Region.Turkish,
            type = AssetType.Stock,
            tradable = true
        ),
        Asset(
            id = "id",
            name = "name",
            symbol = "SISE",
            industryId = "industryId",
            sectorId = "sectorId",
            isActive = true,
            region = Region.Turkish,
            type = AssetType.Stock,
            tradable = true
        )
    )
    @BeforeEach
    fun setup(){
        baseHttpHandler = HTTPHandler(httpURL = "finfree.app")
        stockDataApiProvider = StockDataApiProvider(baseHttpHandler)
        authApiProvider = AuthApiProvider(baseHttpHandler)
        coreApiProvider = CoreApiProvider(baseHttpHandler)
        assetRepo = AssetRepo(MockStorage(), coreApiProvider)
        sessionsRepo = SessionsRepo(
            MockStorage(),
            coreApiProvider,
        ) {
            "Europe/Istanbul"
        }

        sessionProvider = SessionProvider(sessionsRepo =  sessionsRepo)
        assetProvider = AssetProvider(
            assetRepo =  assetRepo,
        )
        priceDataRepo = PriceDataRepo(MockStorage(), stockDataApiProvider, sessionProvider, assetProvider)
        aggregatedPriceDataSeriesRepo = AggregatedPriceDataSeriesRepo(
            stockDataApiProvider,
            MockStorage(),
            priceDataRepo,
        )
    }

    @Test
    fun `Basic usage test`() = runBlocking {
        val loginResponse = authApiProvider.postLogin("test44", "1234qwer")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

       sessionProvider.init()
        assetProvider.init(setOf(Region.Turkish, Region.American))

        val aggregatedPriceDataSeries = aggregatedPriceDataSeriesRepo.getData(
            AggregatedPriceDataSeriesIdentifier(
                assets = testAssetsJustHaveSymbol,
                period = StockDataPeriods.Price1D
            )
        )

        assertNotNull(aggregatedPriceDataSeries)
        val givenTestAssetSymbols = testAssetsJustHaveSymbol.map { it.symbol }
        val responseAssetSymbols = (aggregatedPriceDataSeries?.assets ?: emptyList()).map { it.symbol }
        assertTrue(givenTestAssetSymbols == responseAssetSymbols)
        assertTrue(aggregatedPriceDataSeries?.data?.isNotEmpty() == true)
    }
}
