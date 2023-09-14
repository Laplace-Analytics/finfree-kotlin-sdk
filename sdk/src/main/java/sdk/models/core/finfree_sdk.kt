package sdk.models.core

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import sdk.api.AccessToken
import sdk.api.CoreApiProvider
import sdk.api.RefreshToken
import sdk.api.StockDataApiProvider
import sdk.base.GenericStorage
import sdk.base.exceptions.CoreDataNotInitializedException
import sdk.base.exceptions.InvalidPortfolioTypeException
import sdk.base.exceptions.NotAuthorizedException
import sdk.base.exceptions.PortfolioHandlerNotInitializedException
import sdk.base.exceptions.SDKNotInitializedException
import sdk.base.network.HTTPHandler
import sdk.models.Region
import sdk.repositories.*
import sdk.trade.*
import sdk.trade.models.portfolio.PortfolioHandler
import sdk.trade.models.portfolio.PortfolioProvider
import sdk.base.network.NetworkConfig as network_config

typealias PortfolioType = String
class FinfreeSDK {

    companion object {
        private var _initialized = false
        val initialized get() = _initialized

        private var _storage: GenericStorage? = null
        val storage: GenericStorage
            get() {
                _storage?.let { return it } ?: throw SDKNotInitializedException()
            }

        private var _accessToken: AccessToken? = null
        val authorized: Boolean
            get() = _accessToken != null

        private var _coreInitialized = false
        val coreInitialized get() = _coreInitialized

        val baseHttpHandler: HTTPHandler = HTTPHandler(httpURL = network_config.baseEndpoint)




        private lateinit var authorizationHandler: AuthorizationHandler

        private var _portfolioHandlers: Map<PortfolioType, PortfolioHandler>? = null

        fun portfolioHandler(portfolioType: PortfolioType): PortfolioHandler {
            _portfolioHandlers?.let {
                if (!it.containsKey(portfolioType)) {
                    throw InvalidPortfolioTypeException(portfolioType)
                } else if (it[portfolioType] == null) {
                    throw PortfolioHandlerNotInitializedException()
                }
                return it[portfolioType]!!
            } ?: throw PortfolioHandlerNotInitializedException()
        }


        fun portfolioProvider(portfolioType: PortfolioType): PortfolioProvider =
            portfolioHandler(portfolioType).portfolioProvider

        fun ordersDataHandler(portfolioType: PortfolioType): OrdersDataHandler =
            portfolioHandler(portfolioType).ordersDataHandler
        fun orderHandler(portfolioType: PortfolioType): OrderHandler =
            portfolioHandler(portfolioType).orderHandler


        private var _assetProvider: AssetProvider? = null
        private var _sessionProvider: SessionProvider? = null

        val assetProvider: AssetProvider
            get() {
                if (_assetProvider == null) {
                    throw SDKNotInitializedException()
                }
                return _assetProvider!!
            }

        val sessionProvider: SessionProvider
            get() {
                if (_sessionProvider == null) {
                    throw SDKNotInitializedException()
                }
                return _sessionProvider!!
            }

        private lateinit var coreRepos: CoreRepos


        fun setAccessToken(accessToken: AccessToken) {
            _accessToken = accessToken
            baseHttpHandler.token = accessToken
        }



        fun initSDK(
            getLocalTimezone: GetLocalTimezone,
            storage: GenericStorage,
            portfolioHandlers: Map<PortfolioType, PortfolioHandler>
        ) {
            _storage = storage
            _portfolioHandlers = portfolioHandlers
            authorizationHandler = AuthorizationHandler(storage,baseHttpHandler)

            initializeCoreRepos(getLocalTimezone)
            _assetProvider = AssetProvider(assetRepo = coreRepos.assetRepo)
            _sessionProvider = SessionProvider(sessionsRepo = coreRepos.sessionsRepo)

            _initialized = true
        }

        private fun initializeCoreRepos(getLocalTimezone: GetLocalTimezone) {
            val coreApiProvider = CoreApiProvider(baseHttpHandler)
            coreRepos = CoreRepos(
                assetRepo = AssetRepo(storage, coreApiProvider),
                sessionsRepo = SessionsRepo(storage, coreApiProvider, getLocalTimezone)
            )
        }

        private fun initializePortfolioHandler(
            notifyListeners: () -> Unit,
            showOrderUpdatedMessage: (OrderData) -> Any,
            ordersDBHandler: OrdersDBHandler,
        ) {
            val stockDataApiProvider = StockDataApiProvider(baseHttpHandler, "stock")
            val priceDataRepo = PriceDataRepo(storage, stockDataApiProvider, sessionProvider, assetProvider)


            _portfolioHandlers?.keys?.forEach { portfolioType ->
                portfolioHandler(portfolioType).init(
                    notifyListeners = notifyListeners,
                    showOrderUpdatedMessage = showOrderUpdatedMessage,
                    ordersDBHandler = ordersDBHandler,
                    storage = storage,
                    assetProvider = assetProvider,
                    sessionProvider = sessionProvider,
                    priceDataRepo = priceDataRepo,
                    token = _accessToken!!
                )
            }

        }

        suspend fun userLogin(identifier: String, password: String): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.login(identifier, password)
            response.accessToken?.let {
                setAccessToken(response.accessToken)
            }
            return response
        }

        suspend fun authenticateWithRefreshToken(refreshToken: RefreshToken? = null, tokenId: String? = null): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.authenticateWithRefreshToken(refreshToken, tokenId)
            response.accessToken?.let {
                setAccessToken(response.accessToken)
            }
            return response
        }

        suspend fun initializeCoreData(regions: Set<Region>) {
            if (!initialized) throw SDKNotInitializedException()
            if (!authorized) throw NotAuthorizedException()

            coroutineScope {
                val assetInitialization = async { assetProvider.init(regions) }
                val sessionInitialization = async { sessionProvider.init() }

                assetInitialization.await()
                sessionInitialization.await()
            }
            _coreInitialized = true
        }

        suspend fun initializePortfolioData(
            livePriceDataEnabled: Boolean,
            notifyListeners: () -> Unit,
            showOrderUpdatedMessage:  (OrderData) -> Any,
        ) {
            val ordersDBHandler = MockOrdersDBHandler(assetProvider)

            initializePortfolioHandler(
                notifyListeners = notifyListeners,
                showOrderUpdatedMessage = showOrderUpdatedMessage,
                ordersDBHandler = ordersDBHandler,
            )
            if (!initialized) throw SDKNotInitializedException()
            if (!authorized) throw NotAuthorizedException()
            if (!coreInitialized) throw CoreDataNotInitializedException()

            // TODO insert DB id
            _portfolioHandlers?.keys?.forEach { portfolioType ->
                // TODO insert DB id
                    ordersDataHandler(portfolioType).ordersDBHandler.initDatabase("dbID")
                    portfolioProvider(portfolioType).getUserPortfolio()
                    portfolioProvider(portfolioType).getUserEquityData(
                        ordersDataHandler(portfolioType).getDailyOrders(),
                        true
                    )
            }

        }
    }
}

private data class CoreRepos(
    val assetRepo: AssetRepo,
    val sessionsRepo: SessionsRepo)







