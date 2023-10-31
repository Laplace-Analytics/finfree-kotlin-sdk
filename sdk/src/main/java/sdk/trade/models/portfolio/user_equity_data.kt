package sdk.trade.models.portfolio

import sdk.api.StockDataPeriods
import sdk.base.GenericModel
import sdk.models.Currency
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import sdk.models.data.time_series.EquityDataPoint
import sdk.models.data.time_series.UserEquityTimeSeries
import sdk.models.getCurrencyByAbbreviation
import sdk.models.string

class UserEquityData(
    val equityData: MutableMap<StockDataPeriods, UserEquityTimeSeries>,
    val balances: MutableMap<Currency, Double>,
    val buyingPowers: MutableMap<Currency, Double>,
    val portfolioDetails: MutableMap<Currency, PortfolioSpecificDetails>
) : GenericModel {

    private val tryBalance: Double?
        get() = balances[Currency.tl]

    private val usdBalance: Double?
        get() = balances[Currency.usd]

    private val tryBuyingPower: Double?
        get() = buyingPowers[Currency.tl]

    private val usdBuyingPower: Double?
        get() = buyingPowers[Currency.usd]

    fun balance(currentCurrency: Currency): Double? {
        return when (currentCurrency) {
            Currency.usd -> usdBalance
            Currency.tl -> {
                val tryPortfolioDetails: TRYPortfolioDetails? = (portfolioDetails[Currency.tl] as? TRYPortfolioDetails)
                val cashFundAmount: Double = tryPortfolioDetails?.cashFundAmount ?: 0.0
                return tryBalance!! + cashFundAmount
            }
            Currency.eur -> null
        }
    }

    fun buyingPower(currentCurrency: Currency): Double? {
        return when (currentCurrency) {
            Currency.usd -> usdBuyingPower
            Currency.tl -> tryBuyingPower
            Currency.eur -> null
        }
    }

    fun receiveDataPoint(newData: EquityDataPoint, sessionProvider: SessionProvider): UserEquityData {
        if (!sessionProvider.isDuringMarketHours()) {
            return this
        }

        equityData.forEach { (_, value) ->
            val last = value.data.last()
            value.receiveDataPoint(
                EquityDataPoint(
                    assetsUnderManagement = newData.assetsUnderManagement,
                    index = newData.index,
                    value = newData.value / last.assetsUnderManagement * last.value,
                    timeStamp = newData.time.toEpochMilliSecond(),
                    realizedReturn = last.realizedReturn,
                    unrealizedReturn = last.unrealizedReturn + newData.assetsUnderManagement - last.assetsUnderManagement
                ), sessionProvider
            )
        }
        return this
    }
    fun update(
        equityData: MutableMap<StockDataPeriods, UserEquityTimeSeries>? = null,
        tryBalance: Double? = null,
        tryBuyingPower: Double? = null,
        usdBalance: Double? = null,
        usdBuyingPower: Double? = null,
        portfolioSpecificDetails : PortfolioSpecificDetails? = null,
    ) {
        equityData?.let { this.equityData.putAll(it) }
        tryBalance?.let { balances[Currency.tl] = it }
        tryBuyingPower?.let { buyingPowers[Currency.tl] = it }
        usdBalance?.let { balances[Currency.usd] = it }
        usdBuyingPower?.let { buyingPowers[Currency.usd] = it }
        portfolioSpecificDetails?.let { portfolioDetails[portfolioSpecificDetails.currency] = it }
    }

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "balances" to balances.mapKeys { it.key.string() },
            "buying_powers" to buyingPowers.mapKeys { it.key.string() },
            "equity_data" to equityData.mapValues { it.value.toJson() },
            "portfolio_details" to portfolioDetails.mapValues { it.value.toJson() }
        )
        //control
    }

    companion object {
        fun fromJSON(json: Map<String, Any>): UserEquityData {
            return UserEquityData(
                equityData = (json["equity_data"] as Map<String, Any>).mapKeys { (key, _) ->
                    StockDataPeriods.fromPeriodString(key) ?: throw IllegalArgumentException("Invalid period: $key")
                }.mapValues { (_, value) ->
                    UserEquityTimeSeries.fromJSON(value as Map<String, Any>)
                }.toMutableMap(),

                balances = (json["balances"] as Map<String, Any>).mapNotNull { entry ->
                    val currency = getCurrencyByAbbreviation(entry.key)
                    if (currency == null) {
                        throw Exception("Unknown currency: ${entry.key}")
                    } else {
                        currency to (entry.value as Double)
                    }
                }.toMap().toMutableMap(),

                buyingPowers = (json["buying_powers"] as Map<String, Any>).mapNotNull { entry ->
                    val currency = getCurrencyByAbbreviation(entry.key)
                    if (currency == null) {
                        throw Exception("Unknown currency: ${entry.key}")
                    } else {
                        currency to (entry.value as Double)
                    }
                }.toMap().toMutableMap(),

                portfolioDetails = (json["portfolio_details"] as Map<String, Any>).mapNotNull { entry ->
                    val currency = getCurrencyByAbbreviation(entry.key)
                    if (currency == null) {
                        throw Exception("Unknown currency: ${entry.key}")
                    }
                    when (currency) {
                        Currency.tl -> currency to TRYPortfolioDetails.fromJson(entry.value as Map<String, Any>)
                        Currency.usd -> currency to USDPortfolioDetails.fromJson(entry.value as Map<String, Any>)
                        Currency.eur -> throw Exception("EUR portfolio details not implemented")
                    }
                }.toMap().toMutableMap(),
            )


        }
    }
}

abstract class PortfolioSpecificDetails(
    val currency: Currency
) : GenericModel

class TRYPortfolioDetails(
    val cashFundAmount: Double,
    val cash0: Double,
    val cash1: Double,
    val cash2: Double
) : PortfolioSpecificDetails(currency = Currency.tl) {

    override fun toJson(): Map<String, Any> {
        return mapOf(
            "cash_fund_amount" to cashFundAmount,
            "cash0" to cash0,
            "cash1" to cash1,
            "cash2" to cash2
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>): TRYPortfolioDetails {
            return TRYPortfolioDetails(
                cashFundAmount = json["cash_fund_amount"] as Double,
                cash0 = json["cash0"] as Double,
                cash1 = json["cash1"] as Double,
                cash2 = json["cash2"] as Double
            )
        }
    }
}

class USDPortfolioDetails(
    private val usdWithdrawableAmount: Double,
    private val goodFaithViolationCount: Int?,
    private val patternDayTraderViolationCount: Int?
) : PortfolioSpecificDetails(Currency.usd) {

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "usd_withdrawable_amount" to usdWithdrawableAmount,
            "good_faith_violation_count" to goodFaithViolationCount,
            "pattern_day_trader_violation_count" to patternDayTraderViolationCount
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>): USDPortfolioDetails {
            return USDPortfolioDetails(
                usdWithdrawableAmount = json["usd_withdrawable_amount"] as Double,
                goodFaithViolationCount = json["good_faith_violation_count"] as? Int,
                patternDayTraderViolationCount = json["pattern_day_trader_violation_count"] as? Int
            )
        }
    }
}





