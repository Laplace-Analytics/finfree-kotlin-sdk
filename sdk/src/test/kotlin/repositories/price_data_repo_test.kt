package repositories

import sdk.base.MockStorage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import sdk.models.data.assets.Asset
import sdk.models.data.assets.AssetType
import sdk.models.PriceDataSeries
import sdk.models.data.assets.Region
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.AssetRepo
import sdk.repositories.PriceDataIdentifier
import sdk.repositories.PriceDataRepo
import sdk.repositories.SessionsRepo

class PriceDataRepoTests {
    private lateinit var authApiProvider: AuthApiProvider
    private lateinit var baseHttpHandler: HTTPHandler
    private lateinit var coreApiProvider: CoreApiProvider
    private lateinit var assetRepo: AssetRepo
    private var regionListWithoutTest = mutableListOf<Region>()
    private lateinit var stockDataApiProvider: StockDataApiProvider
    private lateinit var sessionProvider: SessionProvider
    private lateinit var assetProvider: AssetProvider
    private lateinit var priceDataRepo: PriceDataRepo
    private lateinit var sessionsRepo: SessionsRepo

    private val testAssetJustHaveSymbol: Asset = Asset(
    id = "id",
    name= "name",
    symbol= "THYAO",
    industryId= "industryId",
    sectorId= "sectorId",
    isActive= true,
    region= Region.Turkish,
    type= AssetType.Stock,
    )


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
        assetRepo = AssetRepo(MockStorage(), coreApiProvider)
        sessionsRepo = SessionsRepo(
            MockStorage(),
            coreApiProvider,
        ) {
            "Europe/Istanbul"
        }
        stockDataApiProvider = StockDataApiProvider(baseHttpHandler)
        sessionProvider = SessionProvider(sessionsRepo = sessionsRepo)
        assetProvider = AssetProvider(assetRepo = assetRepo)
        priceDataRepo = PriceDataRepo(MockStorage(), stockDataApiProvider, sessionProvider, assetProvider)

    }
    @Test
    fun `Basic usage test`() = runBlocking {
        var priceData: Map<String, Map<StockDataPeriods, PriceDataSeries>>? = priceDataRepo.fetchData(null)
        assertEquals(null, priceData)

        val loginResponse = authApiProvider.postLogin("KereM", "Kfener2002.")
        if (loginResponse.responseType != LoginResponseTypes.SUCCESS) {
            fail("Could not login to Finfree Account")
        }
        val loginData = loginResponse.data!!
        baseHttpHandler.token = loginData.accessToken

        sessionProvider.init()
        assetProvider.init(setOf(Region.Turkish, Region.American))

        priceData = priceDataRepo.fetchData(
            PriceDataIdentifier(
                listOf(testAssetJustHaveSymbol),
                listOf(StockDataPeriods.Price1D)
            )
        )
        assertNotNull(priceData)

        val priceData2 = priceDataRepo.fetchData(
            PriceDataIdentifier(
                listOf(testAssetJustHaveSymbol),
                listOf(StockDataPeriods.Price1D, StockDataPeriods.Price3M)
            )
        )
        assertNotNull(priceData2)
        assertTrue(priceData2!!.entries.first().value.keys.contains(StockDataPeriods.Price1D))
        assertTrue(priceData2.entries.first().value.keys.contains(StockDataPeriods.Price3M))
    }

}