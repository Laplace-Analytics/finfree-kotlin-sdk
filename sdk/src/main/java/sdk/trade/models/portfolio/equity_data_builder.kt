package sdk.trade.models.portfolio

import sdk.api.StockDataPeriods
import sdk.models.Asset
import sdk.models.AssetId
import sdk.models.PriceDataSeries
import sdk.models.core.AssetProvider
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import sdk.models.data.time_series.EquityDataPoint
import sdk.models.data.time_series.UserEquityTimeSeries
import sdk.repositories.PriceDataIdentifier
import sdk.repositories.PriceDataRepo
import sdk.trade.OrderData
import sdk.trade.OrderStatus
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue

class EquityDataBuilder private constructor(
    val orders: List<OrderData>,
    // val bonuses: List<BonusData>,
    val balance: Double,
    val portfolioAssets: Map<AssetId, PortfolioAssetData>,
    // val stockDataHandler: GenericStockDataHandler,
    val priceDataRepo: PriceDataRepo,
    val sessionProvider: SessionProvider,
    val assetProvider: AssetProvider
) {

    companion object {
        fun EquityDataBuilder(
            orders: List<OrderData>,
            // bonuses: List<BonusData>,
            balance: Double,
            portfolioAssets: Map<AssetId, PortfolioAssetData>,
            // stockDataHandler: GenericStockDataHandler,
            priceDataRepo: PriceDataRepo,
            sessionProvider: SessionProvider,
            assetProvider: AssetProvider
        ): EquityDataBuilder {

            val filteredOrders = orders.filter { it.status == OrderStatus.OrderExecuted }

            // val succeededBonuses = bonuses.filter { it.status == BonusStatus.SUCCESS }
            val processedOwnedStocks = portfolioAssets.mapValues {
                PortfolioAssetData(
                    it.value.asset,
                    it.value.quantity,
                    it.value.sellable,
                    it.value.averagePrice,
                    it.value.currentPrice
                )
            }

            return EquityDataBuilder(
                filteredOrders,
                // succeededBonuses,
                balance,
                processedOwnedStocks,
                // stockDataHandler,
                priceDataRepo,
                sessionProvider,
                assetProvider
            )
        }
    }

    suspend fun buildDailyEquityData(livePriceDataEnabled: Boolean): UserEquityTimeSeries {
        val period = StockDataPeriods.Price1D
        val startDate = (sessionProvider.getPreviousTradingDay(date =
        if (livePriceDataEnabled){
            DateTime.now()
        }else{
            DateTime.delayed()
        }))

        val periodInterval = period.periodMinutes
        var currDate = startDate
        val endDate = sessionProvider.getPeriodStart(period, startDate)

        var currStockData = UserPortfolio(
            portfolioAssets.mapValues {
                PortfolioAssetData(
                    it.value.asset,
                    it.value.quantity,
                    it.value.sellable,
                    it.value.averagePrice,
                    it.value.currentPrice
                )
            }.toMutableMap()
        )
        var currBalance = balance

        var periodTransactions = orders
        val priceData = getPriceData(periodTransactions, period)

        val priceCurrIndex = priceData.mapValues { it.value.data.size - 1 }.toMutableMap()
        var transactionsIndex = 0
        val duration = Duration.between(startDate.toInstant(ZoneOffset.UTC), endDate.toInstant(ZoneOffset.UTC))
        var index:Int = (duration.toMinutes().absoluteValue / periodInterval).toInt()
        val dataPoints = mutableListOf<EquityDataPoint>()
        var nextDate: LocalDateTime

        while (currDate.isAfter(endDate)) {
            nextDate = currDate.minusMinutes(periodInterval.toLong())
            val nextTransactionsIndex = transactionsUntil(transactionsIndex, nextDate, periodTransactions)

            val updatedBalanceAndHoldings = processTransactions(transactionsIndex, nextTransactionsIndex, currStockData, currBalance)
            val newBalance = updatedBalanceAndHoldings[0] as Double
            currStockData = updatedBalanceAndHoldings[1] as UserPortfolio

            val newStockValue = getStocksTotalValue(currStockData, priceData, priceCurrIndex, nextDate)
            val newEquity = newBalance + newStockValue

            dataPoints.add(0,
                EquityDataPoint(
                    index = index,
                    timeStamp = currDate.toEpochMilliSecond(),
                    value = newEquity,
                    assetsUnderManagement = newEquity,
                    unrealizedReturn = 0.0,
                    realizedReturn = 0.0
                )
            )

            currBalance = newBalance
            transactionsIndex = nextTransactionsIndex
            currDate = nextDate
            index--
        }
        var totalInitialValue = 0.0

        currStockData.portfolioAssets.forEach { (key, value) ->
            totalInitialValue += (priceData[key]?.initialValue ?: value.averagePrice) * value.quantity.toDouble()
        }
        val ratio = (currBalance + totalInitialValue) / 100

        if (ratio == 0.0) {
            for (i in dataPoints.indices) {
                val lastDataPoint = dataPoints.getOrNull(i - 1)
                val e = dataPoints[i]
                val unrealizedReturn = lastDataPoint?.let {
                    e.assetsUnderManagement - it.assetsUnderManagement + it.unrealizedReturn
                } ?: (e.assetsUnderManagement - currBalance - totalInitialValue)

                dataPoints[i] = EquityDataPoint(
                    assetsUnderManagement = e.assetsUnderManagement,
                    index = e.index,
                    value = 100.0,
                    timeStamp = e.time.toEpochMilliSecond(),
                    realizedReturn = e.realizedReturn,
                    unrealizedReturn = unrealizedReturn
                )
            }
        } else {
            for (i in dataPoints.indices) {
                val lastDataPoint = dataPoints.getOrNull(i - 1)
                val e = dataPoints[i]
                val unrealizedReturn = lastDataPoint?.let {
                    e.assetsUnderManagement - it.assetsUnderManagement + it.unrealizedReturn
                } ?: (e.assetsUnderManagement - currBalance - totalInitialValue)

                dataPoints[i] = EquityDataPoint(
                    assetsUnderManagement = e.assetsUnderManagement,
                    index = e.index,
                    value = e.value / ratio,
                    timeStamp = e.time.toEpochMilliSecond(),
                    realizedReturn = e.realizedReturn,
                    unrealizedReturn = unrealizedReturn
                )
            }
        }

        return UserEquityTimeSeries(dataPoints, period = period, initialValue = 100.0)
    }

    private fun getStocksTotalValue(
        userStockData: UserPortfolio,
        priceData: Map<AssetId, PriceDataSeries>,
        priceCurrIndices: MutableMap<AssetId, Int>,
        date: LocalDateTime
    ): Double {
        var stocksTotalValue = 0.0

        userStockData.portfolioAssets.forEach { (assetId, ownedStockData) ->
            if (!priceCurrIndices.containsKey(assetId) || !priceData.containsKey(assetId) || priceData[assetId] == null) {
                return@forEach
            }

            val currentIndex = getPriceDataIndexAtTime(priceData[assetId]!!, date, priceCurrIndices[assetId]!!)
            priceCurrIndices[assetId] = currentIndex
            stocksTotalValue += (if (currentIndex >= priceData[assetId]!!.data.size || currentIndex < 0) {
                priceData[assetId]!!.initialValue
            } else {
                priceData[assetId]!!.data[currentIndex].value
            }) * ownedStockData.quantity.toDouble()
        }

        return stocksTotalValue
    }

    private suspend fun getPriceData(periodTransactions: List<OrderData>, period: StockDataPeriods): Map<AssetId, PriceDataSeries> {
        val assets = mutableListOf<Asset>()

        for (transaction in periodTransactions) {
            assets.add(transaction.asset)
        }

        portfolioAssets.forEach { (key, value) ->
            if (assets.none { it.id == key }) {
                assets.add(value.asset)
            }
        }

        if (assets.isEmpty()) return emptyMap()

        val priceData: Map<AssetId, Map<StockDataPeriods, PriceDataSeries>>? = priceDataRepo.getData(
            PriceDataIdentifier(assets, listOf(period))
        )

        if (priceData == null) return emptyMap()

        return priceData.mapValues { (_, value) -> value[period]!! }
    }

    private fun transactionsUntil(index: Int, nextDate: LocalDateTime, currTransactions: List<OrderData>): Int {
        var i = index
        while (i < currTransactions.size && (currTransactions[i].executed == null || currTransactions[i].executed!!.isAfter(nextDate))) {
            i++
        }
        return i
    }

    private fun processTransactions(fromIndex: Int, toIndex: Int, userStockData: UserPortfolio, equity: Double): List<Any> {
        var currentEquity = equity

        if (orders.isEmpty() || fromIndex == toIndex) {
            return listOf(currentEquity, userStockData)
        }

        for (i in fromIndex until toIndex) {
            val currTransaction = orders[i]
            if (currTransaction != null){
                val executionPrice = currTransaction.price!!
                if (currTransaction.isSell) {
                    userStockData.buy(currTransaction.asset, currTransaction.quantity, executionPrice)
                    currentEquity -= executionPrice * currTransaction.quantity.toDouble()
                } else {
                    userStockData.sell(currTransaction.asset, currTransaction.quantity, executionPrice)
                    currentEquity += executionPrice * currTransaction.quantity.toDouble()
                }
            }
        }

        return listOf(currentEquity, userStockData)
    }
    private fun getPriceDataIndexAtTime(data: PriceDataSeries, time: LocalDateTime, startIndex: Int? = null): Int {
        var currIndex = startIndex ?: (data.data.size - 1)

        while (currIndex > 0 && data.data[currIndex].time.isAfter(time)) {
            currIndex--
        }

        return currIndex
    }
}
