package sdk.trade.models.portfolio

import io.reactivex.subjects.BehaviorSubject
import sdk.base.tryNTimesIfResultIsNull
import sdk.trade.GenericPortfolioApiProvider
import sdk.trade.OrderData
import sdk.trade.repositories.repos.PortfolioRepoIdentifier
import sdk.trade.repositories.repos.UserEquityRepo
import sdk.trade.repositories.repos.UserPortfolioRepo

class PortfolioProvider(
    private val portfolioRepo: UserPortfolioRepo,
    private val userEquityDataRepo: UserEquityRepo<out GenericPortfolioApiProvider>
) {
    private val _userEquityDataStream = BehaviorSubject.create<UserEquityData>()
    private val _userPortfolioStream = BehaviorSubject.create<UserPortfolio>()

    val userPortfolio: UserPortfolio?
        get() = _userPortfolioStream.value

    val equityData: UserEquityData?
        get() = _userEquityDataStream.value

    val awaitUserPortfolio: UserPortfolio
        get() = _userPortfolioStream.firstOrError().blockingGet()

    val awaitEquityData: UserEquityData
        get() = _userEquityDataStream.firstOrError().blockingGet()

    suspend fun getUserPortfolio(): UserPortfolio? {
        val result = portfolioRepo.getData(null)
        result?.let {
            _userPortfolioStream.onNext(it)
        }
        return result
    }

    suspend fun fetchUserPortfolio(): UserPortfolio? {
        val result = tryNTimesIfResultIsNull(
            callback = {
                portfolioRepo.fetchData(null)
            }
        )
        result?.let {
            _userPortfolioStream.onNext(it)
        }
        return result
    }


    suspend fun getUserEquityData(
        dailyTransactions: List<OrderData>,
        livePriceDataEnabled: Boolean
    ): UserEquityData? {
        if (userPortfolio == null) {
            getUserPortfolio()
            if (userPortfolio == null) return null
        }

        val result = userEquityDataRepo.getData(
            PortfolioRepoIdentifier(
                dailyTransactions,
                userPortfolio!!.portfolioAssets,
                livePriceDataEnabled = livePriceDataEnabled
            )
        )

        result?.let {
            _userEquityDataStream.onNext(it)
        }

        return result
    }

    suspend fun fetchUserEquityData(
        dailyTransactions: List<OrderData>,
        livePriceDataEnabled: Boolean
    ): UserEquityData? {
        if (userPortfolio == null) {
            getUserPortfolio()
            if (userPortfolio == null) return null
        }

        val result = userEquityDataRepo.fetchData(
            PortfolioRepoIdentifier(
                dailyTransactions,
                userPortfolio!!.portfolioAssets,
                livePriceDataEnabled = livePriceDataEnabled
            )
        )

        result?.let {
            _userEquityDataStream.onNext(it)
        }

        return result
    }
}
