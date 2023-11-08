package sdk.models.core

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import sdk.api.AccessToken
import sdk.api.AuthApiProvider
import sdk.api.CoreApiProvider
import sdk.api.LoginResponseData
import sdk.api.RefreshToken
import sdk.api.StockDataApiProvider
import sdk.base.GenericStorage
import sdk.base.exceptions.CoreDataNotInitializedException
import sdk.base.exceptions.InvalidPortfolioTypeException
import sdk.base.exceptions.NotAuthorizedException
import sdk.base.exceptions.OrderDBHandlerNotInitializedException
import sdk.base.exceptions.PortfolioHandlerNotInitializedException
import sdk.base.exceptions.SDKNotInitializedException
import sdk.base.network.HTTPHandler
import sdk.models.data.account.AccountData
import sdk.models.data.assets.Content
import sdk.models.data.assets.PortfolioType
import sdk.models.data.assets.Region
import sdk.repositories.*
import sdk.trade.*
import sdk.trade.models.order.OrdersDBHandler
import sdk.trade.models.order.OrdersDataHandler
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

        private var _accountData: AccountData? = null
        private val accountData: AccountData
            get() = _accountData ?: throw Exception("Account data is not initialized")
        val authorized: Boolean
            get() = _accessToken != null && _accountData != null

        private var _coreInitialized = false
        val coreInitialized get() = _coreInitialized

        val portfolioInitialized: Boolean
            get() = portfolioHandlers != null && portfolioHandlers!!.isNotEmpty()

        val baseHttpHandler: HTTPHandler = HTTPHandler(httpURL = network_config.baseEndpoint)


        private var coreRepos: CoreRepos? = null

        private val authorizationHandler: AuthorizationHandler = AuthorizationHandler(storage, baseHttpHandler)

        private var portfolioHandlers: Map<PortfolioType, PortfolioHandler>? = null

        fun portfolioHandler(portfolioType: PortfolioType): PortfolioHandler {
            portfolioHandlers?.let {
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

        val data: CoreDataProviders
            get() {
                val stockDataApiProvider = StockDataApiProvider(baseHttpHandler)
                val priceDataRepo = PriceDataRepo(storage, stockDataApiProvider, sessionProvider, assetProvider)
                return CoreDataProviders(
                    priceDataRepo,
                    AggregatedPriceDataSeriesRepo(stockDataApiProvider,storage,priceDataRepo),
                    AssetCollectionDetailRepo(storage, CoreApiProvider(baseHttpHandler)),
                    AssetCollectionRepo(storage, CoreApiProvider(baseHttpHandler)),
                    assetProvider,
                    sessionProvider,
                )
            }


        fun setAccessToken(accessToken: AccessToken) {
            _accessToken = accessToken
            baseHttpHandler.token = accessToken
        }



        fun initSDK(
            getLocalTimezone: GetLocalTimezone,
            storage: GenericStorage,
        ) {
            _storage = storage
            initializeCoreRepos(getLocalTimezone)
            if (coreRepos == null) throw SDKNotInitializedException()
            _assetProvider = AssetProvider(assetRepo = coreRepos!!.assetRepo)
            _sessionProvider = SessionProvider(sessionsRepo = coreRepos!!.sessionsRepo)
            _initialized = true
        }

        private fun initializeCoreRepos(getLocalTimezone: GetLocalTimezone) {
            val coreApiProvider = CoreApiProvider(baseHttpHandler)
            coreRepos = CoreRepos(
                assetRepo = AssetRepo(storage, coreApiProvider),
                sessionsRepo = SessionsRepo(storage, coreApiProvider, getLocalTimezone),
            )
        }

        private suspend fun initializePortfolioHandlers(
            showOrderUpdatedMessage: (OrderData) -> Any,
            ordersDBHandlers: Map<PortfolioType, OrdersDBHandler?>,
            hasLiveData: ((Content) -> Boolean)?
        ) {

            portfolioHandlers?.keys?.forEach { portfolioType ->
                portfolioHandler(portfolioType).init(
                    showOrderUpdatedMessage = showOrderUpdatedMessage,
                    ordersDBHandler = ordersDBHandlers[portfolioType]!!,
                    storage = storage,
                    assetProvider = assetProvider,
                    sessionProvider = sessionProvider,
                    priceDataRepo = data.priceDataRepo,
                    token = _accessToken!!,
                    hasLiveData = hasLiveData
                )
            }

        }

        suspend fun getFinfreeLoginData(): LoginResponseData? {
            return authorizationHandler.getFinfreeLoginData()
        }

        suspend fun userLogin(identifier: String, password: String): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.login(identifier, password)
            response.accessToken?.let {
                setAccessToken(response.accessToken)
                initializeAccountData()
                if (_accountData == null) {
                    _accessToken = null
                    return AuthenticationResponse(AuthenticationResponseTypes.UnknownError, "account data is null", response.accessToken)
                }
            }
            return response
        }

        suspend fun authenticateWithRefreshToken(refreshToken: RefreshToken? = null, tokenId: String? = null): AuthenticationResponse {
            if (!initialized) throw SDKNotInitializedException()
            val response = authorizationHandler.authenticateWithRefreshToken(refreshToken, tokenId)
            response.accessToken?.let {
                setAccessToken(response.accessToken)
                initializeAccountData()
                if (_accountData == null) {
                    _accessToken = null
                    return AuthenticationResponse(AuthenticationResponseTypes.UnknownError, "account data is null", response.accessToken)
                }
            }
            return response
        }

        fun logout(){
            _accessToken = null
            baseHttpHandler.token = null
            _accountData = null

            _coreInitialized = false
            portfolioHandlers = null
            authorizationHandler.logout()

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

        private suspend fun initializeAccountData() {
            val authApiProvider = AuthApiProvider(baseHttpHandler)

            val accountDataRepo = AccountDataRepo(
                authApiProvider,
                storage
            )
            val accountDataResponse = accountDataRepo.getData(null)
            if (accountDataResponse != null) {
                _accountData = accountDataResponse
            }
        }

        suspend fun initializePortfolioData(
            showOrderUpdatedMessage:  (OrderData) -> Any,
            portfolioHandlers: Map<PortfolioType, PortfolioHandler>,
            ordersDBHandlers: Map<PortfolioType, OrdersDBHandler?>,
            hasLiveData: ((Content) -> Boolean)?
        ) {
            if (!initialized) throw SDKNotInitializedException()
            if (!authorized) throw NotAuthorizedException()
            if (!coreInitialized) throw CoreDataNotInitializedException()
            this.portfolioHandlers = portfolioHandlers

            this.portfolioHandlers?.let { portfolioHandlers ->
                portfolioHandlers.keys.forEach { key ->
                    if (!ordersDBHandlers.containsKey(key) || ordersDBHandlers[key] == null) {
                        throw OrderDBHandlerNotInitializedException(key)
                    }
                }
            } ?: throw PortfolioHandlerNotInitializedException()


            initializePortfolioHandlers(
                showOrderUpdatedMessage = showOrderUpdatedMessage,
                ordersDBHandlers = ordersDBHandlers,
                hasLiveData = hasLiveData
            )

            // TODO insert DB id
            this.portfolioHandlers?.keys?.forEach { portfolioType ->
                // TODO insert DB id
                ordersDataHandler(portfolioType).ordersDBHandler.initDatabase()
                coroutineScope {
                    async { portfolioProvider(portfolioType).getUserPortfolio() }.await()
                    async { portfolioProvider(portfolioType).getUserEquityData(
                        ordersDataHandler(portfolioType).getDailyOrders(),
                        true
                    )
                    }.await()
                }
            }
        }
}

private data class CoreRepos(
    val assetRepo: AssetRepo,
    val sessionsRepo: SessionsRepo,
)

data class CoreDataProviders(
    val priceDataRepo: PriceDataRepo,
    val aggregatedPriceDataSeriesRepo: AggregatedPriceDataSeriesRepo,
    val assetCollectionDetailRepo: AssetCollectionDetailRepo,
    val assetCollectionRepo: AssetCollectionRepo,
    val assetProvider: AssetProvider,
    val sessionProvider: SessionProvider
)
}








