package sdk.models.core

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import sdk.api.AccessToken
import sdk.api.CoreApiProvider
import sdk.api.StockDataApiProvider
import sdk.base.GenericStorage
import sdk.base.exceptions.CoreDataNotInitializedException
import sdk.base.exceptions.NotAuthorizedException
import sdk.base.exceptions.SDKNotInitializedException
import sdk.base.network.HTTPHandler
import sdk.models.Region
import sdk.repositories.*
import sdk.trade.*
import sdk.trade.models.portfolio.PortfolioHandler
import sdk.trade.models.portfolio.PortfolioProvider
import sdk.base.network.NetworkConfig as network_config

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

        private lateinit var authorizationHandler: AuthorizationHandler

        private var _portfolioHandler: PortfolioHandler? = null

        val portfolioHandler: PortfolioHandler
            get() {
                if (_portfolioHandler == null) {
                    throw SDKNotInitializedException()
                }
                return _portfolioHandler!!
            }

        val portfolioProvider: PortfolioProvider
            get() {

                if (portfolioHandler.portfolioProvider == null) {
                    throw SDKNotInitializedException()
                }
                return portfolioHandler.portfolioProvider!!
            }

        val ordersDataHandler: OrdersDataHandler
            get() {

                if (portfolioHandler.ordersDataHandler == null) {
                    throw SDKNotInitializedException()
                }
                return portfolioHandler.ordersDataHandler!!
            }

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

        val orderHandler: OrderHandler
            get() = portfolioHandler.orderHandler

        fun initSDK(
            getLocalTimezone: GetLocalTimezone,
            storage: GenericStorage,
            portfolioHandler: PortfolioHandler
        ) {
            _storage = storage
            _portfolioHandler = portfolioHandler
            authorizationHandler = AuthorizationHandler(storage,baseHttpHandler)

            initializeCoreRepos(getLocalTimezone)
            _assetProvider = AssetProvider(assetRepo = coreRepos.assetRepo, assetCollectionRepo = coreRepos.assetCollectionRepo)
            _sessionProvider = SessionProvider(sessionsRepo = coreRepos.sessionsRepo)

            _initialized = true
        }

        private fun initializeCoreRepos(getLocalTimezone: GetLocalTimezone) {
            val coreApiProvider = CoreApiProvider(baseHttpHandler)
            coreRepos = CoreRepos(
                assetRepo = AssetRepo(storage, coreApiProvider),
                assetCollectionRepo = AssetCollectionRepo(storage, coreApiProvider),
                sessionsRepo = SessionsRepo(storage, coreApiProvider, getLocalTimezone)
            )
        }

        private fun initializePortfolioHandler(
            notifyListeners: () -> Unit,
            showOrderUpdatedMessage: (OrderData) -> Any,
            ordersDBHandler: OrdersDBHandler,
            storage: GenericStorage
        ) {
            val stockDataApiProvider = StockDataApiProvider(baseHttpHandler, "stock")
            val priceDataRepo = PriceDataRepo(storage, stockDataApiProvider, sessionProvider, assetProvider)

            portfolioHandler.init(
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

        suspend fun userLogin(identifier: String, password: String): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.login(identifier, password)
            response.accessToken?.let {
                setAccessToken(response.accessToken)
            }
            return response
        }

        suspend fun authenticateWithRefreshToken(): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.authenticateWithRefreshToken()
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
            val ordersDBHandler = MockOrdersDBHandler(_assetProvider!!)

            initializePortfolioHandler(
                notifyListeners = notifyListeners,
                showOrderUpdatedMessage = showOrderUpdatedMessage,
                ordersDBHandler = ordersDBHandler,
                storage = storage
            )


            if (!initialized) throw SDKNotInitializedException()
            if (!authorized) throw NotAuthorizedException()
            if (!coreInitialized) throw CoreDataNotInitializedException()

            // TODO insert DB id
            ordersDataHandler.ordersDBHandler.initDatabase("dbID")
            portfolioProvider.getUserPortfolio()
            portfolioProvider.getUserEquityData(
                ordersDataHandler.getDailyOrders(),
                true
            )
        }
        val baseHttpHandler: HTTPHandler = HTTPHandler(httpURL = network_config.baseEndpoint)
        fun setAccessToken(accessToken: AccessToken) {
            _accessToken = accessToken
            baseHttpHandler.token = accessToken
        }
    }
}

private data class CoreRepos(
    val assetRepo: AssetRepo,
    val assetCollectionRepo: AssetCollectionRepo,
    val sessionsRepo: SessionsRepo)







