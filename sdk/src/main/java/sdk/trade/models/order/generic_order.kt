package sdk.trade

import sdk.base.GenericModel
import sdk.models.data.assets.Asset
import sdk.models.core.AssetProvider
import sdk.models.core.sessions.DateTime
import sdk.models.core.sessions.DateTime.Companion.toEpochMilliSecond
import java.time.LocalDateTime

enum class OrderStatus {
    TransmittingToExchange,
    TransmittedToExchange,
    OutOfLimit,
    InsufficientBalance,
    IncorrectOrder,
    OrderPeriodOver,
    OrderExecuted,
    OrderCancelled,
    SendingCancelRequestToExchange,
    SentCancelRequestToExchange,
    OrderCancellationRefused,
    OrderIsBeingCorrected,
    MainOrderIsBeingCorrected,
    OldOrder,
    IncorrectCorrection,
    StockIsNotTraded,
    UnspecifiedOrderStatus,
    MarketOrderDidNotExecute,
    FractionalOrderNotSupported,
    MarketClosed,
    UnsupportedStepValue,
    ServerError,
    PartialFill,
    AccountPending,
    AccountClosed,
    AccountClosePositionsOnly,
    AccountMarkedAsPatternDayTrader,
    OrderAmountLessThanMinimum,
    OrderAmountMoreThanMaximum,
    OrderQuantityMoreThanMaximum,
    LimitPriceLessThanMinimum
}

enum class OrderIdType {
    Int, String
}

abstract class OrderId(val type: OrderIdType) {
    abstract val id: String

    companion object {
        fun fromString(id: String): OrderId = StringOrderId(id)

        fun fromInt(id: Int): OrderId = IntOrderId(id)

        fun fromValue(value: Any?): OrderId {
            return when (value) {
                is String -> StringOrderId(value)
                is Int -> IntOrderId(value)
                else -> throw Exception("Invalid OrderId type")
            }
        }
    }
}

class StringOrderId(private val _id: String) : OrderId(OrderIdType.String) {
    override val id: String
        get() = _id
}

class IntOrderId(private val _id: Int) : OrderId(OrderIdType.Int) {
    override val id: String
        get() = _id.toString()
}

enum class OrderType {
    LimitBuy,
    LimitSell,
    MarketBuy,
    MarketSell
}
enum class OrderSource {
    Virtual,
    TrReal,
    DriveWealth,
}

data class OrderData(
    val orderId: OrderId,
    val orderNo: String?,
    val orderType: OrderType,
    val orderSource: OrderSource,
    val status: OrderStatus,
    val errorCode: String?,
    val statusMessage: String?,
    val asset: Asset,
    val quantity: Number,
    val remainingQuantity: Number,
    val price: Double?,
    val limitPrice: Double?,
    val placed: LocalDateTime,
    val executed: LocalDateTime?
) : GenericModel {
    private val _errorCode = errorCode
    private val _statusMessage = statusMessage

    val isBuy: Boolean
        get() = orderType == OrderType.MarketBuy || orderType == OrderType.LimitBuy

    val isSell: Boolean
        get() = !isBuy

    override fun toString(): String {
        return "${asset.symbol}: $orderType, $quantity, $status"
    }

    override fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to orderId.id,
            "stock_id" to asset.id,
            "price_executed" to price,
            "price_ordered" to limitPrice,
            "quantity" to quantity,
            "remaining_quantity" to remainingQuantity,
            "executed_date" to (executed?.toEpochMilliSecond() ?: placed.toEpochMilliSecond()),
            "placed_date" to placed.toEpochMilliSecond(),
            "status" to status.ordinal,
            "error_code" to _errorCode,
            "status_message" to _statusMessage,
            "order_type" to orderType.ordinal,
            "order_no" to orderNo,
            "order_source" to orderSource.ordinal
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any>, assetProvider: AssetProvider): OrderData {
            val asset: Asset? = if (json["stock_id"] != null) {
                assetProvider.findById(json["stock_id"] as String)
            } else if (json["symbol"] != null) {
                assetProvider.findBySymbol(json["symbol"] as String)
            } else {
                null
            }

            if(asset == null){
                throw Exception("Asset not found")
            }

            return OrderData(
                orderId = OrderId.fromValue(json["id"]!!),
                orderNo = json["order_no"] as String,
                asset = asset,
                status = OrderStatus.values()[json["status"] as Int],
                errorCode = json["error_code"] as String,
                statusMessage = json["status_message"] as String,
                orderType = OrderType.values()[json["order_type"] as Int],
                price = json["price_executed"] as Double,
                quantity = json["quantity"] as Double,
                remainingQuantity = json["remaining_quantity"] as Double,
                limitPrice = json["price_ordered"] as Double,
                placed = DateTime.fromSinceEpochMilliSecond(json["placed_date"] as Long),
                executed = json["executed_date"]?.let { DateTime.fromSinceEpochMilliSecond(it as Long) },
                orderSource = OrderSource.values()[json["order_source"] as Int]
            )
        }
    }

    fun copyWith(
        orderId: OrderId? = null,
        orderNo: String? = null,
        orderType: OrderType? = null,
        status: OrderStatus? = null,
        orderSource: OrderSource? = null,
        _errorCode: String? = null,
        _statusMessage: String? = null,
        asset: Asset? = null,
        quantity: Number? = null,
        remainingQuantity: Number? = null,
        price: Double? = null,
        limitPrice: Double? = null,
        placed: LocalDateTime? = null,
        executed: LocalDateTime? = null
    ): OrderData {
        return OrderData(
            orderId = orderId ?: this.orderId,
            orderNo = orderNo ?: this.orderNo,
            orderType = orderType ?: this.orderType,
            status = status ?: this.status,
            orderSource = orderSource ?: this.orderSource,
            errorCode = _errorCode ?: this._errorCode,
            statusMessage = _statusMessage ?: this._statusMessage,
            asset = asset ?: this.asset,
            quantity = quantity ?: this.quantity,
            remainingQuantity = remainingQuantity ?: this.remainingQuantity,
            price = price ?: this.price,
            limitPrice = limitPrice ?: this.limitPrice,
            placed = placed ?: this.placed,
            executed = executed ?: this.executed
        )
    }
}

val OrderStatus.isPositive: Boolean?
    get() = orderStatusPositive[this]

private val orderStatusPositive = mapOf(
    OrderStatus.MarketOrderDidNotExecute to false,
    OrderStatus.SentCancelRequestToExchange to null,
    OrderStatus.SendingCancelRequestToExchange to null,
    OrderStatus.StockIsNotTraded to false,
    OrderStatus.OldOrder to false,
    OrderStatus.IncorrectCorrection to false,
    OrderStatus.OrderCancelled to false,
    OrderStatus.OrderExecuted to true,
    OrderStatus.OrderPeriodOver to false,
    OrderStatus.IncorrectOrder to false,
    OrderStatus.InsufficientBalance to false,
    OrderStatus.OutOfLimit to false,
    OrderStatus.MainOrderIsBeingCorrected to null,
    OrderStatus.OrderIsBeingCorrected to null,
    OrderStatus.TransmittingToExchange to null,
    OrderStatus.TransmittedToExchange to null,
    OrderStatus.OrderCancellationRefused to false,
    OrderStatus.UnspecifiedOrderStatus to false,
    OrderStatus.ServerError to false
)



