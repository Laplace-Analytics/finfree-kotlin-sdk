package sdk.trade.models.order

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sdk.base.logger
import sdk.models.core.SessionProvider
import sdk.models.core.sessions.DateTime
import sdk.trade.OrderData
import sdk.trade.OrderId
import sdk.trade.OrderStatus
import sdk.trade.OrdersDBHandler
import sdk.trade.repositories.repos.OrdersRepository
import sdk.trade.repositories.repos.PaginatedOrdersFilter
import java.lang.Integer.max
import java.time.Duration
import java.util.*

typealias VoidCallback = () -> Unit


val realTradeNonFinalOrderStatus = listOf(
    OrderStatus.SentCancelRequestToExchange,
    OrderStatus.SendingCancelRequestToExchange,
    OrderStatus.IncorrectCorrection,
    OrderStatus.MainOrderIsBeingCorrected,
    OrderStatus.OrderIsBeingCorrected,
    OrderStatus.TransmittingToExchange,
    OrderStatus.TransmittedToExchange,
    OrderStatus.OrderCancellationRefused,
    OrderStatus.PartialFill
)

class OrdersDataHandler(
    private val showOrderUpdatedMessage: (OrderData) -> Any,
    private val ordersRepository: OrdersRepository,
    private val fetchUserStockDataCallback: AsyncCallback,
    private val fetchUserEquityDataCallback: AsyncCallback,
    val ordersDBHandler: OrdersDBHandler,
    private val sessionProvider: SessionProvider,
) {

    private val listeners = mutableListOf<VoidCallback>()

    fun addListener(listener: VoidCallback) {
        listeners.add(listener)
    }

    fun removeListener(listener: VoidCallback) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        for (listener in listeners) {
            try {
                listener()
            } catch (e: Exception) {
                logger.error("Error occurred while OrdersDataHandler's _notifyListeners $e")
            }
        }
    }



    private var timer: Timer? = null
    private var isTimerActive = false


    private suspend fun updateOrders() {
        notifyListeners()
    }

    /**
     * Initializes a timer to fetch recent orders and check for any change.
     */
    suspend fun initialFetch() {
        val numTransactions = ordersDBHandler.getNumberOfOrders()
        if (numTransactions == 0) {
            fetchAll()
        } else {
            val orders = ordersDBHandler.filterOrders(limit = 1)
            val firstOrder = orders.first()
            val limitDate = sessionProvider.getDayStart(
                date = sessionProvider.getDayStart(
                    date = sessionProvider.getPreviousTradingDay(
                        date = firstOrder.placed.minus(Duration.ofDays(1))
                    )
                )
            )
            val index = ordersDBHandler.getOrdersSinceDate(limitDate).size
            if (index >= 1) {
                fetchN(N = index)
            }
        }
        isTimerActive = true
        timer?.scheduleAtFixedRate(object : TimerTask() {

            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    fetchPeriodic()
                }
            }
        }, 0, Duration.ofMinutes(1).toMillis())
    }

    suspend fun fetchPeriodic() {
        var limitDate = sessionProvider.getDayStart(
            date = DateTime.now().minusHours(10)
        )
        val oldestPendingOrder = ordersDBHandler.getOldestPendingOrder()?.placed ?: DateTime.now()
        if (oldestPendingOrder.isBefore(limitDate)) {
            limitDate = oldestPendingOrder
        }
        timer?.let {
            if (isTimerActive && ordersDBHandler.isOpen) {
                val index = ordersDBHandler.getOrdersSinceDate(limitDate).size
                if (index != 0) {
                    fetchN(N = index)
                } else {
                    fetchN(N = 5)
                }
            }
        }
    }

    private suspend fun fetchAll() {
        val res = ordersRepository.getData(null)
        if (res != null) {
            ordersDBHandler.insertOrders(res)
            updateOrders()
        } else {
            logger.error("Transaction data is null")
        }
    }

    private suspend fun fetchN(from: Int = 0, N: Int? = null) {
        val nValue = max(N ?: 5, 5)
        logger.info("Fetching last $nValue orders")
        val orders = ordersRepository.getData(PaginatedOrdersFilter(from, from + nValue))
        if (orders != null) {
            processFetchedData(orders, realTradeNonFinalOrderStatus)
        } else {
            logger.info("No orders returned")
        }
    }

    suspend fun getDailyOrders(): List<OrderData> {
        val previousClose = sessionProvider
            .getDayStart(date = sessionProvider.getPreviousTradingDay(date = DateTime.now()))
            .minusHours(16)
        return ordersDBHandler.getOrdersSinceDate(previousClose)
    }

    private suspend fun processFetchedData(orders: List<OrderData>, pendingStatus: List<OrderStatus>) {
        insertFetchedData(orders, pendingStatus)
    }

    private fun getMapFromOrderList(orders: List<OrderData>): Map<OrderId, OrderData> {
        return orders.associateBy { it.orderId }
    }

    private suspend fun checkIfAnyOrderUpdated(orders: List<OrderData>): TransactionCheckResult {
        if (orders.isEmpty()) {
            return TransactionCheckResult(updateUI = false, fetchData = false)
        }

        if (orders.size == 1 && realTradeNonFinalOrderStatus.contains(orders.first().status)) {
            return TransactionCheckResult(updateUI = true, fetchData = false)
        }

        val ordersSincePlacedDate = ordersDBHandler.getOrdersSincePlacedDate(
            orders.first().placed.minusMinutes(5)
        )

        val filteredTransactionMap = getMapFromOrderList(ordersSincePlacedDate)

        for (order in orders) {
            val correspondingTransaction = filteredTransactionMap[order.orderId]
                ?: return TransactionCheckResult(updateUI = true, fetchData = true)
            if (order.status != correspondingTransaction.status ||
                order.quantity != correspondingTransaction.quantity ||
                order.remainingQuantity != correspondingTransaction.remainingQuantity
            ) {
                return TransactionCheckResult(updateUI = true, fetchData = true)
            }
        }

        return TransactionCheckResult(updateUI = false, fetchData = false)
    }

    private suspend fun insertFetchedData(orders: List<OrderData>, pendingStatus: List<Any>) {
        if (ordersDBHandler.isOpen) {
            val matchedTransactions = ordersDBHandler.filterOrders(
                ids = orders.filterNot { pendingStatus.contains(it.status) }.map { it.orderId },
                limit = orders.size
            )

            matchedTransactions.filter { pendingStatus.contains(it.status) }.forEach { oldOrder ->
                val matchedOrder = orders.firstOrNull { order -> order.orderId == oldOrder.orderId }
                if (matchedOrder != null && oldOrder.status != matchedOrder.status) {
                    orderStatusChanged(oldOrder, matchedOrder)
                }
            }
            val checkResult = checkIfAnyOrderUpdated(orders)
            if (checkResult.updateUI && ordersDBHandler.isOpen) {
                ordersDBHandler.insertOrders(orders)
                updateOrders()
                if (checkResult.fetchData) {
                    fetchUserStockDataCallback()
                    fetchUserEquityDataCallback()
                }
            }
        }
    }

    private fun orderStatusChanged(oldTransaction: OrderData, newTransaction: OrderData) {
        if (oldTransaction.status == OrderStatus.TransmittingToExchange) {
            when (newTransaction.status) {
                OrderStatus.OrderPeriodOver -> {}
                OrderStatus.OrderExecuted -> showTransactionStatusChangedMessage(newTransaction)
            }
        } else {
            logger.info("Transaction status changed from type ${oldTransaction.status} to type ${newTransaction.status}")
        }
    }

    private fun showTransactionStatusChangedMessage(order: OrderData) {
        showOrderUpdatedMessage(order)
    }

    fun dispose() {
        timer?.cancel()
        ordersDBHandler.dispose()
    }

    suspend fun improveOrderUpdateDB(
        orderId: OrderId,
        newQuantity: Number,
        newPrice: Double,
        remainingQuantity: Number
    ) {
        ordersDBHandler.updateOrderFields(
            orderId = orderId,
            newQuantity = newQuantity,
            newPrice = newPrice,
            remainingQuantity = remainingQuantity
        )
        updateOrders()
    }

    suspend fun cancelOrderUpdateDB(orderID: OrderId) {
        ordersDBHandler.updateOrderFields(orderID, status = OrderStatus.OrderCancelled)
        updateOrders()
    }

    suspend fun updateOrder(updatedOrder: OrderData, fetchUserStock: Boolean = false) {
        ordersDBHandler.insertOrders(listOf(updatedOrder))
        updateOrders()
        if (fetchUserStock) {
            fetchUserStockDataCallback()
            fetchUserEquityDataCallback()
        }
    }

    suspend fun checkIfRealOrderStatusChanged(order: OrderData) {
        val updatedOrder = ordersRepository.getOrderByID(order.orderId.id)
        if (updatedOrder != null && !realTradeNonFinalOrderStatus.contains(updatedOrder.status)) {
            updateOrder(updatedOrder, fetchUserStock = true)
        }
    }
}

typealias AsyncCallback = suspend () -> Unit

private data class TransactionCheckResult(
    val updateUI: Boolean,
    val fetchData: Boolean
)