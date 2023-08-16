package sdk.models.core

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import sdk.api.AccessToken
import sdk.api.CoreApiProvider
import sdk.api.StockDataApiProvider
import sdk.base.GenericStorage
import sdk.base.network.HTTPHandler
import sdk.models.Region
import sdk.repositories.*
import sdk.trade.*
import sdk.trade.generic_api.DriveWealthPortfolioApiProvider
import sdk.base.network.NetworkConfig as network_config

class FinfreeSDK {

    companion object {
        private var _initialized = false
        val initialized get() = _initialized

        private var _authorized = false
        val authorized get() = _authorized

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
            notifyListeners: () -> Unit,
            showOrderUpdatedMessage: (OrderData) -> Any
        ) {
            authorizationHandler = AuthorizationHandler(storage, HttpHandlers.baseHttpHandler)

            initializeCoreRepos(storage, getLocalTimezone)
            _assetProvider = AssetProvider(assetRepo = coreRepos.assetRepo, assetCollectionRepo = coreRepos.assetCollectionRepo)
            _sessionProvider = SessionProvider(sessionsRepo = coreRepos.sessionsRepo)

            // TODO: Gerçek DB handler oluştur
            val ordersDBHandler = MockOrdersDBHandler(_assetProvider!!)

            initializePortfolioHandler(
                notifyListeners = notifyListeners,
                showOrderUpdatedMessage = showOrderUpdatedMessage,
                ordersDBHandler = ordersDBHandler,
                storage = storage
            )

            _initialized = true
        }

        private fun initializeCoreRepos(storage: GenericStorage, getLocalTimezone: GetLocalTimezone) {
            val coreApiProvider = CoreApiProvider(HttpHandlers.baseHttpHandler)
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
            val orderAPIProvider = DriveWealthOrderAPIProvider(HttpHandlers.dwHttpHandler, "order")
            val stockDataApiProvider = StockDataApiProvider(HttpHandlers.baseHttpHandler, "stock")
            val portfolioAPIProvider = DriveWealthPortfolioApiProvider(HttpHandlers.dwHttpHandler, "/api/v1/tr")
            val priceDataRepo = PriceDataRepo(storage, stockDataApiProvider, sessionProvider, assetProvider)

            portfolioHandler.init(
                notifyListeners = notifyListeners,
                showOrderUpdatedMessage = showOrderUpdatedMessage,
                ordersDBHandler = ordersDBHandler,
                storage = storage,
                orderAPIProvider = orderAPIProvider,
                assetProvider = assetProvider,
                sessionProvider = sessionProvider,
                priceDataRepo = priceDataRepo,
                portfolioAPIProvider = portfolioAPIProvider
            )
        }

        suspend fun userLogin(identifier: String, password: String): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.login(identifier, password)
            response.accessToken?.let {
                HttpHandlers.setAccessToken(it)
                _authorized = true
            }
            return response
        }

        suspend fun authenticateWithRefreshToken(): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.authenticateWithRefreshToken()
            response.accessToken?.let {
                HttpHandlers.setAccessToken(it)
                _authorized = true
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

        suspend fun initializePortfolioData(livePriceDataEnabled: Boolean) {
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
    }
}

private data class CoreRepos(
    val assetRepo: AssetRepo,
    val assetCollectionRepo: AssetCollectionRepo,
    val sessionsRepo: SessionsRepo
)

class HttpHandlers {
    companion object {
        val baseHttpHandler: HTTPHandler = HTTPHandler(httpURL = network_config.baseEndpoint)

        val dwHttpHandler: HTTPHandler = HTTPHandler(httpURL = network_config.driveWealthProdURL).apply {
            constantHeaders["X-GEDIK-ACCOUNT-ID"] = "941380"
        }

        fun setAccessToken(accessToken: AccessToken) {
            baseHttpHandler.token = accessToken
            dwHttpHandler.token = accessToken
        }
    }
}

open class InitializationException(override val message: String) : Exception() {
    override fun toString(): String {
        return "InitializationException[${this::class.simpleName}]: $message"
    }
}

class SDKNotInitializedException : InitializationException("SDK not initialized, should call FinfreeSDK.initSDK() first")

class NotAuthorizedException : InitializationException("SDK not authorized for user yet, should call FinfreeSDK.userLogin() or FinfreeSDK.authenticateWithRefreshToken() first")

class CoreDataNotInitializedException : InitializationException("Core data not initialized, should call FinfreeSDK.initializeCoreData() first")



