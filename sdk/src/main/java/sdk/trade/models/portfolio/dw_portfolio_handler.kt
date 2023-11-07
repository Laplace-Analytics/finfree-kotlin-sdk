package sdk.trade.models.portfolio

import sdk.api.AccessToken
import sdk.base.GenericStorage
import sdk.base.exceptions.PortfolioHandlerNotInitializedException
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.PortfolioType
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.models.data.assets.Content
import sdk.repositories.PriceDataRepo
import sdk.trade.api.drivewealth_api.DriveWealthOrderAPIProvider
import sdk.trade.repositories.drivewealth_repos.DriveWealthOrdersRepository
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserEquityRepo
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserPortfolioRepo
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.OrderData
import sdk.trade.OrderHandler
import sdk.trade.OrderId
import sdk.trade.models.order.OrderUpdatesHandler
import sdk.trade.OrderUpdatesListener
import sdk.trade.OrdersDBHandler
import sdk.trade.models.order.OrdersDataHandler
import sdk.trade.repositories.repos.OrdersRepository
import sdk.trade.repositories.repos.UserEquityRepo
import sdk.trade.repositories.repos.UserPortfolioRepo
import sdk.trade.generic_api.DriveWealthPortfolioApiProvider
import sdk.trade.repositories.drivewealth_repos.DriveWealthOrderHandler
import sdk.trade.repositories.repos.EquityTimeSeriesRepo

class DWPortfolioHandler(
    override val endpointUrl: String,
    ) : PortfolioHandler(endpointUrl) {
    override val portfolioType: PortfolioType = PortfolioType.DriveWealth

    private var _orderUpdatesHandler: OrderUpdatesHandler? = null
    override val orderUpdatesHandler: OrderUpdatesHandler
        get() {
            return _orderUpdatesHandler ?: throw PortfolioHandlerNotInitializedException()
        }

    private var _ordersDataHandler: OrdersDataHandler? = null
    override val ordersDataHandler: OrdersDataHandler
        get() {
            return _ordersDataHandler ?: throw PortfolioHandlerNotInitializedException()
        }

    private var _portfolioProvider: PortfolioProvider? = null
    override val portfolioProvider: PortfolioProvider
        get() {
            return _portfolioProvider ?: throw PortfolioHandlerNotInitializedException()
        }

    private var _orderHandler: OrderHandler? = null
    override val orderHandler: OrderHandler
        get() {
            return _orderHandler ?: throw PortfolioHandlerNotInitializedException()
        }

    private var portfolioRepos: PortfolioRepos? = null

    private val hasLiveData: Boolean
        get() = true

    override suspend fun init(
        showOrderUpdatedMessage: (OrderData) -> Any,
        ordersDBHandler: OrdersDBHandler,
        storage: GenericStorage,
        assetProvider: AssetProvider,
        sessionProvider: SessionProvider,
        priceDataRepo: PriceDataRepo,
        token: AccessToken,
        hasLiveData: ((Content) -> Boolean)?
    ) {
        val httpHandler = HTTPHandler(httpURL = endpointUrl)
        httpHandler.token = token
        val portfolioAPIProvider = DriveWealthPortfolioApiProvider(httpHandler, "api/v1/tr")
        val orderAPIProvider = DriveWealthOrderAPIProvider(httpHandler, "/api/v1/tr/order")

        portfolioRepos = PortfolioRepos(
            ordersRepo = DriveWealthOrdersRepository(storage, orderAPIProvider, assetProvider),
            priceDataRepo = priceDataRepo,
            userEquityRepo = DriveWealthUserEquityRepo(storage, portfolioAPIProvider, priceDataRepo, sessionProvider, assetProvider),
            userPortfolioRepo = DriveWealthUserPortfolioRepo(storage, portfolioAPIProvider, assetProvider),
            equityTimeSeriesRepo =  EquityTimeSeriesRepo(
                sessionProvider,
                storage,
                portfolioAPIProvider,
        ),
        )

        _ordersDataHandler = OrdersDataHandler(
            showOrderUpdatedMessage = showOrderUpdatedMessage,
            ordersRepository = portfolioRepos!!.ordersRepo,
            fetchUserStockDataCallback = {
                // TODO implement
            },
            fetchUserEquityDataCallback = {
                // TODO implement
            },
            ordersDBHandler = ordersDBHandler,
            sessionProvider = sessionProvider,
        )

        val orderUpdatesListener = OrderUpdatesListener(
            portfolioRepos!!.ordersRepo.apiProvider
        )

        _orderUpdatesHandler = OrderUpdatesHandler(
            orderUpdatesListener,
            ordersDataHandler,
            portfolioRepos!!.ordersRepo,
            assetProvider
        )

        _portfolioProvider = PortfolioProvider(
            portfolioRepo = portfolioRepos!!.userPortfolioRepo,
            userEquityDataRepo = portfolioRepos!!.userEquityRepo,
            equityTimeSeriesRepo =  portfolioRepos!!.equityTimeSeriesRepo,
            ordersRepo = portfolioRepos!!.ordersRepo
        )

        _orderHandler = DriveWealthOrderHandler(
            orderAPIProvider = portfolioRepos!!.ordersRepo.apiProvider
        )
    }

    override fun dispose() {
        orderUpdatesHandler.close()
        // ordersDataHandler?.dispose()
        // portfolio.dispose()
    }

    override suspend fun getUserPortfolio(): UserPortfolio? {
        return portfolioProvider.getUserPortfolio()
    }

    override suspend fun fetchUserPortfolio(): UserPortfolio? {
        return portfolioProvider.fetchUserPortfolio()
    }

    override suspend fun fetchTransactionsPeriodic() {
        ordersDataHandler.fetchPeriodic()
    }

    override suspend fun improveOrderUpdateDB(
        transactionID: OrderId,
        newQuantity: Number,
        newPrice: Double,
        remainingQuantity: Number
    ) {
        ordersDataHandler.improveOrderUpdateDB(transactionID, newQuantity, newPrice, remainingQuantity)
    }

    override suspend fun cancelOrder(orderID: OrderId) {
        ordersDataHandler.cancelOrderUpdateDB(orderID)
    }

    override suspend fun getUserEquityData(): UserEquityData? {
        val dailyOrders = ordersDataHandler.getDailyOrders()
        return portfolioProvider.getUserEquityData(dailyOrders, hasLiveData)
    }

    override suspend fun fetchUserEquityData(): UserEquityData? {
        val dailyOrders = ordersDataHandler.getDailyOrders()
        return portfolioProvider.fetchUserEquityData(dailyOrders, hasLiveData)
    }

    override fun addUserPortfolio(data: UserPortfolio) {
        portfolioProvider.addUserPortfolio(data)
    }
}

private class PortfolioRepos(
    val ordersRepo: OrdersRepository,
    val priceDataRepo: PriceDataRepo,
    val userEquityRepo: UserEquityRepo<out GenericPortfolioApiProvider>,
    val userPortfolioRepo: UserPortfolioRepo,
    val equityTimeSeriesRepo: EquityTimeSeriesRepo<out GenericPortfolioApiProvider>
)

