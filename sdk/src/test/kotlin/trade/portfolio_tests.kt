package trade

import initSDK
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import sdk.base.logger
import sdk.models.data.assets.PortfolioType
import sdk.models.core.FinfreeSDK
import sdk.trade.models.portfolio.UserPortfolio
import java.util.Random

class PortfolioTests{
    private val portfolioType: PortfolioType = PortfolioType.DriveWealth

    @BeforeEach
    fun setup(){
        runBlocking {
            handleSetup()
        }
    }

    @Test
    fun `Get Portfolio`() {
        val portfolio = FinfreeSDK.portfolioProvider(portfolioType).userPortfolio
        assertNotNull(portfolio)
        assertTrue(portfolio!!.portfolioAssets.isEmpty())
    }


    @Test
    fun `Non Empty Portfolio Tests`() {
        runBlocking {

            val assetsToPurchase = listOf("AAPL", "MSFT", "VTI", "TSLA")
                .mapNotNull { FinfreeSDK.assetProvider.findBySymbol(it) }

            val random = Random()

            assetsToPurchase.forEach { asset ->
                FinfreeSDK.orderHandler(portfolioType).postMarketOrder(
                    quantity = 1.0 / (random.nextInt(100) + 10),
                    asset = asset
                )
            }

            delay(5000)
            FinfreeSDK.portfolioProvider(portfolioType).fetchUserPortfolio()
        }

        val portfolio = FinfreeSDK.portfolioProvider(portfolioType).userPortfolio
        assertNotNull(portfolio)
        assertTrue(portfolio!!.portfolioAssets.isNotEmpty())
    }

}

suspend fun handleSetup() = runBlocking {
    val portfolioType: PortfolioType = PortfolioType.DriveWealth
    initSDK(portfolioType)
    FinfreeSDK.initializePortfolioData(
        livePriceDataEnabled = true,
        notifyListeners = {
            logger.info("Notify listeners")
        },
        showOrderUpdatedMessage = { order ->
            logger.info("Order updated: $order")
        }
    )

    var portfolio: UserPortfolio? = FinfreeSDK.portfolioProvider(portfolioType).userPortfolio
        ?: throw Exception("Portfolio is null")
    while (portfolio!!.portfolioAssets.isNotEmpty()) {
        GlobalScope.launch {
            val tasks = portfolio!!.portfolioAssets.entries.map { entry ->
                async {
                    FinfreeSDK.orderHandler(portfolioType).postMarketOrder(quantity = 0 - entry.value.quantity.toDouble(), asset = entry.value.asset)
                }
            }
            tasks.forEach { it.await() }
        }


        delay(5000)

        FinfreeSDK.portfolioProvider(portfolioType).fetchUserPortfolio()
        portfolio = FinfreeSDK.portfolioProvider(portfolioType).userPortfolio
    }
}