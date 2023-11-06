package sdk.trade.models.portfolio

import sdk.api.AccessToken
import sdk.base.GenericStorage
import sdk.models.data.assets.PortfolioType
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.models.data.assets.Content
import sdk.repositories.PriceDataRepo
import sdk.trade.OrderData
import sdk.trade.OrderHandler
import sdk.trade.OrderId
import sdk.trade.models.order.OrderUpdatesHandler
import sdk.trade.OrdersDBHandler
import sdk.trade.OrdersDataHandler


abstract class PortfolioHandler(open val endpointUrl: String) {
    open val portfolioType: PortfolioType = PortfolioType.Virtual

    abstract val orderUpdatesHandler: OrderUpdatesHandler
    abstract val ordersDataHandler: OrdersDataHandler
    abstract val portfolioProvider: PortfolioProvider
    abstract val orderHandler: OrderHandler


    abstract fun dispose()

    abstract suspend fun init(
        notifyListeners: () -> Any,
        showOrderUpdatedMessage: (OrderData) -> Any,
        ordersDBHandler: OrdersDBHandler,
        storage: GenericStorage,
        assetProvider: AssetProvider,
        sessionProvider: SessionProvider,
        priceDataRepo: PriceDataRepo,
        token: AccessToken,
        hasLiveData: ((Content) -> Boolean)? = null
    )

    abstract suspend fun getUserPortfolio() : UserPortfolio?

    abstract suspend fun fetchUserPortfolio(): UserPortfolio?

    abstract suspend fun fetchTransactionsPeriodic()

    abstract suspend fun improveOrderUpdateDB(
        transactionID: OrderId,
        newQuantity: Number,
        newPrice: Double,
        remainingQuantity: Number
    )

    abstract suspend fun cancelOrder(orderID: OrderId)

    abstract suspend fun getUserEquityData(): UserEquityData?

    abstract suspend fun fetchUserEquityData(): UserEquityData?
}
