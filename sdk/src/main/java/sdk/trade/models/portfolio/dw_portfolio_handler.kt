package sdk.trade.models.portfolio

import sdk.api.AccessToken
import sdk.base.GenericStorage
import sdk.base.exceptions.PortfolioHandlerNotInitializedException
import sdk.base.network.HTTPHandler
import sdk.models.data.assets.PortfolioType
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.PriceDataRepo
import sdk.trade.api.drivewealth_api.DriveWealthOrderAPIProvider
import sdk.trade.repositories.drivewealth_repos.DriveWealthOrdersRepository
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserEquityRepo
import sdk.trade.repositories.drivewealth_repos.DriveWealthUserPortfolioRepo
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.OrderData
import sdk.trade.OrderHandler
import sdk.trade.OrderId
import sdk.trade.OrderUpdatesHandler
import sdk.trade.OrderUpdatesListener
import sdk.trade.OrdersDBHandler
import sdk.trade.OrdersDataHandler
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

    private var _portfolioRepos: PortfolioRepos? = null

    override suspend fun init(
        notifyListeners: () -> Any,
        showOrderUpdatedMessage: (OrderData) -> Any,
        ordersDBHandler: OrdersDBHandler,
        storage: GenericStorage,
        assetProvider: AssetProvider,
        sessionProvider: SessionProvider,
        priceDataRepo: PriceDataRepo,
        token: AccessToken
    ) {
        val httpHandler = HTTPHandler(httpURL = endpointUrl)
        httpHandler.token = token
        val portfolioAPIProvider = DriveWealthPortfolioApiProvider(httpHandler, "api/v1/tr")
        val orderAPIProvider = DriveWealthOrderAPIProvider(httpHandler, "/api/v1/tr/order")

        _portfolioRepos = PortfolioRepos(
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
            ordersRepository = _portfolioRepos!!.ordersRepo,
            fetchUserStockDataCallback = {
                // TODO implement
            },
            fetchUserEquityDataCallback = {
                // TODO implement
            },
            ordersDBHandler = ordersDBHandler,
            sessionProvider = sessionProvider,
            notifyListeners = notifyListeners
        )

        val orderUpdatesListener = OrderUpdatesListener(
            _portfolioRepos!!.ordersRepo.apiProvider
        )

        _orderUpdatesHandler = OrderUpdatesHandler(
            orderUpdatesListener,
            ordersDataHandler,
            _portfolioRepos!!.ordersRepo,
            assetProvider
        )

        _portfolioProvider = PortfolioProvider(
            portfolioRepo = _portfolioRepos!!.userPortfolioRepo,
            userEquityDataRepo = _portfolioRepos!!.userEquityRepo,
            equityTimeSeriesRepo =  _portfolioRepos!!.equityTimeSeriesRepo,
        )

        _orderHandler = DriveWealthOrderHandler(
            orderAPIProvider = _portfolioRepos!!.ordersRepo.apiProvider
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

    override suspend fun getUserEquityData(livePriceDataEnabled: Boolean): UserEquityData? {
        val dailyOrders = ordersDataHandler.getDailyOrders()
        return portfolioProvider.getUserEquityData(dailyOrders, livePriceDataEnabled)
    }

    override suspend fun fetchUserEquityData(livePriceDataEnabled: Boolean): UserEquityData? {
        val dailyOrders = ordersDataHandler.getDailyOrders()
        return portfolioProvider.fetchUserEquityData(dailyOrders, livePriceDataEnabled)
    }
}

private class PortfolioRepos(
    val ordersRepo: OrdersRepository,
    val priceDataRepo: PriceDataRepo,
    val userEquityRepo: UserEquityRepo<out GenericPortfolioApiProvider>,
    val userPortfolioRepo: UserPortfolioRepo,
    val equityTimeSeriesRepo: EquityTimeSeriesRepo<out GenericPortfolioApiProvider>
)

