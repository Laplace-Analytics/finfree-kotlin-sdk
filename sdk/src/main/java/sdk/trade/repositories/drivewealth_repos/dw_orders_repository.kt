package sdk.trade.repositories.drivewealth_repos

import sdk.base.GenericStorage
import sdk.base.getDoubleFromDynamic
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.models.core.AssetProvider
import sdk.trade.GenericOrderAPIProvider
import sdk.trade.OrderData
import sdk.trade.OrderId
import sdk.trade.OrderSource
import sdk.trade.OrderStatus
import sdk.trade.OrderType
import sdk.trade.repositories.repos.OrdersRepository
import sdk.trade.repositories.repos.PaginatedOrdersFilter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DriveWealthOrdersRepository(
    storageHandler: GenericStorage,
    apiProvider: GenericOrderAPIProvider,
    assetProvider: AssetProvider
) : OrdersRepository(storageHandler, apiProvider, assetProvider) {

    override suspend fun getOrderByID(id: String): OrderData? {
        val response = apiProvider.getOrder(id)
        if (response.responseType != BasicResponseTypes.Success || response.data == null) {
            return null
        }

        return try {
            val data = response.data
            orderDataFromJSON(data)
        } catch (ex: Exception) {
            // Assuming logger is defined somewhere
            logger.error("Error while processing orders json: ${response.data}", ex)
            null
        }
    }
    override fun orderDataFromJSON(json: Map<String, Any>): OrderData {
        val asset = assetProvider.findBySymbol(json["symbol"] as String)
            ?: throw Exception("Asset not found")


        val isBuy = json["side"] == "BUY"
        val orderType = json["type"]
        val transactionType = when(orderType) {
            "MARKET" -> if (isBuy) OrderType.MarketBuy else OrderType.MarketSell
            "LIMIT" -> if (isBuy) OrderType.LimitBuy else OrderType.LimitSell
            else -> null
        } ?: throw Exception("Unknown orderType or side: $json")

        val price = getDoubleFromDynamic(json["average_price"]) ?: throw Exception("Executed was null: $json")
        val quantity = getDoubleFromDynamic(json["quantity"]) ?: throw Exception("Quantity was null: $json")
        val executedQuantity = getDoubleFromDynamic(json["executed_quantity"]) ?: throw Exception("executedQuantity quantity was null: $json")

        val remainingQuantity = quantity - executedQuantity

        val placed = json["created_at"]?.let { val instant = Instant.parse(it as String)
            instant.atZone(ZoneId.systemDefault()).toLocalDateTime() }
            ?: throw Exception("Placed date was null: $json")

        return OrderData(
            orderId = OrderId.fromValue(json["order_id"]),
            orderNo = json["order_no"] as String,
            asset = asset,
            status = getOrderStatus(json["status"] as String),
            errorCode = json["error_code"] as String,
            statusMessage = json["status_message"] as String,
            orderType = transactionType,
            price = price,
            quantity = quantity,
            remainingQuantity = remainingQuantity,
            limitPrice = getDoubleFromDynamic(json["order_price"]),
            placed = placed,
            executed = json["order_date"]?.let { val instant = Instant.parse(it as String)
                instant.atZone(ZoneId.systemDefault()).toLocalDateTime() },
            orderSource = OrderSource.DriveWealth
        )
    }

    fun getOrderStatus(statusCode: String): OrderStatus {
        return orderStatusCodes[statusCode] ?: OrderStatus.UnspecifiedOrderStatus
    }

    override suspend fun fetchData(identifier: PaginatedOrdersFilter?): List<OrderData>? {
        val from = identifier?.from ?: 1
        val N = 50
        val to = identifier?.to ?: (from + N - 1)

        val response = apiProvider.getTransactionsBetween(from, to)

        if (response.responseType != BasicResponseTypes.Success || response.data == null) {
            return null
        }

        return try {
            getFromJson(mapOf("orders" to response.data))
        } catch (ex: Exception) {
            logger.error("Error while processing orders json: ${response.data}", ex)
            null
        }
    }
    override suspend fun getData(identifier: PaginatedOrdersFilter?): List<OrderData>? {
        return fetchData(identifier)
    }

    override fun getFromJson(json: Map<String, Any>): List<OrderData> {
        val orders = json["orders"] as List<Map<String, Any>>
        return orders.map { orderDataFromJSON(it) }
    }

    override fun getIdentifier(data: List<OrderData>): PaginatedOrdersFilter? {
        throw NotImplementedError()
    }

    override fun getPath(identifier: PaginatedOrdersFilter?): String {
       return "drivewealth_orders"
    }

    override fun toJson(data: List<OrderData>): Map<String, Any> {
        return mapOf(
            "orders" to data.map { it.toJson() }
        )
    }

}
private val orderStatusCodes: Map<String, OrderStatus> = mapOf(
    "FILLED" to OrderStatus.OrderExecuted,
    "PARTIAL_FILL" to OrderStatus.PartialFill,
    "REJECTED" to OrderStatus.IncorrectOrder,
    "CANCELED" to OrderStatus.OrderCancelled,
    "NEW" to OrderStatus.TransmittingToExchange,
    "PENDING_CANCEL" to OrderStatus.SentCancelRequestToExchange
)
