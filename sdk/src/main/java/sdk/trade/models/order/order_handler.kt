package sdk.trade

import sdk.models.data.assets.Asset

abstract class OrderHandler(
    open val orderAPIProvider: GenericOrderAPIProvider
) {
    abstract suspend fun postMarketOrder(
        quantity: Double,
        asset: Asset,
        isNotionalOrder: Boolean = false
    ): OrderResponse

    abstract suspend fun postLimitOrder(
        quantity: Int,
        limitPrice: Double,
        asset: Asset
    ): OrderResponse

    abstract suspend fun cancelOrder(orderId: OrderId): DeleteOrderResponse

    abstract suspend fun improveOrder(orderId: OrderId, asset: Asset, newPrice: Double, newQuantity: Int): OrderResponse

    abstract suspend fun fetchPeriodic()
}

data class OrderResponse(
    val id:OrderId? = null,
    val responseType: OrderStatus,
    val message: String? = null
)
