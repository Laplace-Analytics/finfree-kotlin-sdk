package sdk.trade.repositories.drivewealth_repos

import sdk.api.StockDataPeriods
import sdk.base.GenericStorage
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.Currency
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.repositories.PriceDataRepo
import sdk.trade.generic_api.DriveWealthPortfolioApiProvider
import sdk.trade.models.portfolio.EquityDataBuilder.Companion.createEquityDataBuilder
import sdk.trade.models.portfolio.PortfolioSpecificDetails
import sdk.trade.models.portfolio.USDPortfolioDetails
import sdk.trade.models.portfolio.UserEquityData
import sdk.trade.repositories.repos.PortfolioRepoIdentifier
import sdk.trade.repositories.repos.UserEquityRepo

class DriveWealthUserEquityRepo(
    storageHandler: GenericStorage,
    apiProvider: DriveWealthPortfolioApiProvider,
    priceDataRepo: PriceDataRepo,
    sessionProvider: SessionProvider,
    assetProvider: AssetProvider
) : UserEquityRepo<DriveWealthPortfolioApiProvider>(
    storageHandler,
    apiProvider,
    priceDataRepo,
    sessionProvider,
    assetProvider
) {
    override suspend fun fetchData(identifier: PortfolioRepoIdentifier?): UserEquityData? {
        identifier ?: run {
            logger.error("PortfolioRepoIdentifier is null")
            return null
        }

        val response = apiProvider.getPortfolio()
        if (response.responseType != BasicResponseTypes.Success || response.data == null) {
            logger.error("Error while fetching Gedik US portfolio data: ${response.message}")
            return null
        }

        val data = response.data
        val usdBalance = (data["cash_balance"] as? Number)?.toDouble() ?: 0.0
        val usdBuyingPower = when (val value = data["cash_available_for_trade"]) {
            is String -> value.toDouble()
            is Number -> value.toDouble()
            else -> 0.0
        }
        val withdrawableAmount = when (val value = data["cash_available_for_withdrawal"]) {
            is String -> value.toDouble()
            is Number -> value.toDouble()
            else -> 0.0
        }

        val goodFaithViolationCount = data["good_faith_violation_count"] as? Int
        val patternDayTraderViolationCount = data["pattern_day_trades_violation_count"] as? Int

        val eq = createEquityDataBuilder(
            orders = identifier.orderData,
            balance = usdBalance,
            priceDataRepo = priceDataRepo,
            sessionProvider = sessionProvider,
            assetProvider = assetProvider,
            portfolioAssets = identifier.portfolioAssets
        )

        val equityData = mapOf(
            StockDataPeriods.Price1D to eq.buildDailyEquityData(identifier.livePriceDataEnabled)
        ).toMutableMap()

        val balances = mutableMapOf<Currency, Double>()
        balances[Currency.usd] = usdBalance

        val buyingPowers = mutableMapOf<Currency, Double>()
        buyingPowers[Currency.usd] = usdBuyingPower

        val portfolioDetails = mutableMapOf<Currency, PortfolioSpecificDetails>()
        portfolioDetails[Currency.usd] = USDPortfolioDetails(
            withdrawableAmount,
            goodFaithViolationCount,
            patternDayTraderViolationCount
        )

        return UserEquityData(
            equityData = equityData,
            balances = balances,
            buyingPowers = buyingPowers,
            portfolioDetails = portfolioDetails
        )
    }

    override fun getPath(identifier: PortfolioRepoIdentifier?): String {
        throw NotImplementedError("Not implemented yet.")
    }

    override fun getIdentifier(data: UserEquityData): PortfolioRepoIdentifier? {
        throw NotImplementedError("Not implemented yet.")
    }

    override fun getFromJson(json: Map<String, Any>): UserEquityData {
        throw NotImplementedError("Not implemented yet.")
    }


    override fun toJson(data: UserEquityData): Map<String, Any> {
        throw NotImplementedError("Not implemented yet.")
    }

}
