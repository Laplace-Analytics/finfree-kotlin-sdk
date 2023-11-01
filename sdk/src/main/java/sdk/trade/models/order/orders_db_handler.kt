package sdk.trade

import sdk.models.AssetId
import sdk.models.core.AssetProvider
import java.lang.Math.min
import java.time.LocalDateTime
import java.util.*

abstract class OrdersDBHandler(open val assetProvider: AssetProvider) {

    abstract val initialized: Boolean

    abstract suspend fun initDatabase(dbID: String)

    abstract suspend fun insertOrders(orders: List<OrderData>)

    abstract val isOpen: Boolean

    abstract suspend fun filterOrders(
        assetIds: List<AssetId>? = null,
        orderTypes: List<OrderType>? = null,
        orderStatus: List<OrderStatus>? = null,
        ids: List<OrderId>? = null,
        offset: Int = 0,
        limit: Int = 5
    ): List<OrderData>

    abstract suspend fun getOrdersSinceDate(date: LocalDateTime): List<OrderData>

    abstract suspend fun getOrdersSincePlacedDate(date: LocalDateTime): List<OrderData>

    abstract suspend fun getDistinctSymbols(): List<AssetId>

    abstract suspend fun getNumberOfOrders(): Int

    abstract suspend fun getOldestPendingOrder(): OrderData?

    abstract suspend fun updateOrderFields(
        orderId: OrderId,
        newQuantity: Number? = null,
        newPrice: Double? = null,
        status: OrderStatus? = null,
        remainingQuantity: Number? = null
    )

    abstract fun dispose()
}

val random = Random()

class MockOrdersDBHandler(
    override val assetProvider: AssetProvider,
) : OrdersDBHandler(assetProvider) {
    private lateinit var mockData: MutableList<OrderData>

    override fun dispose() {
        // TODO: implement dispose
    }

    override suspend fun filterOrders(
        assetIds: List<AssetId>?,
        orderTypes: List<OrderType>?,
        orderStatus: List<OrderStatus>?,
        ids: List<OrderId>?,
        offset: Int,
        limit: Int
    ): List<OrderData> {
        val filtered = mockData.filter { order ->
            (assetIds == null || assetIds.contains(order.asset.id)) &&
                    (orderTypes == null || orderTypes.contains(order.orderType)) &&
                    (orderStatus == null || orderStatus.contains(order.status)) &&
                    (ids == null || ids.contains(order.orderId))
        }

        return filtered.subList(offset, min(offset + limit, filtered.size))
    }

    override suspend fun getDistinctSymbols(): List<AssetId> {
        return mockData.map { it.asset.symbol }.toSet().toList()
    }

    override suspend fun getNumberOfOrders(): Int {
        return mockData.size
    }

    override suspend fun getOldestPendingOrder(): OrderData? {
        return mockData.firstOrNull { realTradeNonFinalOrderStatus.contains(it.status) }
    }


    override suspend fun getOrdersSinceDate(date: LocalDateTime): List<OrderData> {
        return mockData.filter { it.executed != null && it.executed.isAfter(date) }
    }

    override suspend fun getOrdersSincePlacedDate(date: LocalDateTime): List<OrderData> {
        return mockData.filter { it.placed.isAfter(date) }
    }

    override suspend fun initDatabase(dbID: String) {
        mockData = List(200) { index ->
            val quantity = random.nextInt(100) + 1
            OrderData(
                orderId = IntOrderId(index),
                asset = assetProvider.allAssets[index + random.nextInt(10)],
                orderType = OrderType.values()[index % 2],
                status = OrderStatus.values()[index % 3],
                placed = LocalDateTime.now().minusDays(index.toLong()),
                price = random.nextInt(100) / 10.0 + index / 10.0,
                quantity = quantity,
                remainingQuantity = random.nextInt(quantity),
                limitPrice = random.nextInt(100) / 10.0 + index / 10.0,
                executed = LocalDateTime.now().minusDays(index.toLong()),
                errorCode = random.nextInt(100).toString(),
                statusMessage = "error message $index",
                orderNo = "orderNo$index",
                orderSource = OrderSource.DriveWealth
            )
        }.toMutableList()
        sortOrders()
    }

    override suspend fun insertOrders(orders: List<OrderData>) {
        mockData.addAll(orders)
        sortOrders()
    }
    fun sortOrders() {
        mockData.sortBy { it.placed }
    }
    override suspend fun updateOrderFields(
        orderId: OrderId,
        newQuantity: Number?,
        newPrice: Double?,
        status: OrderStatus?,
        remainingQuantity: Number?
    ) {
        val order = mockData.firstOrNull { it.orderId == orderId } ?: throw Exception("Order not found")

        newQuantity?.let { order.copy(quantity = it) }
        newPrice?.let { order.copy(limitPrice = it) }
        status?.let { order.copy(status = it) }
        remainingQuantity?.let { order.copy(remainingQuantity = it) }
    }
    override val initialized: Boolean
        get() = TODO("Not yet implemented")

    override val isOpen: Boolean
        get() = TODO("Not yet implemented")
}
