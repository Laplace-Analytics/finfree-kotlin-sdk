package sdk.trade

import kotlinx.coroutines.delay
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.models.Asset

class OrderHandler(
    val orderAPIProvider: GenericOrderAPIProvider
) {

    private fun orderStatusFromResponse(
        responseType: BasicResponseTypes,
        message: String? = null
    ): OrderStatus {
        // TODO: Implement error handling
        return when (responseType) {
            BasicResponseTypes.Success -> OrderStatus.TransmittedToExchange
            BasicResponseTypes.Error -> OrderStatus.UnspecifiedOrderStatus
        }
    }

    private suspend fun handleOrderResponse(orderResponse: BasicResponse<Map<String, Any>>): OrderResponse {
        val id = orderResponse.data?.get("orderID") as OrderId
        val message = orderResponse.message
        val responseType = orderStatusFromResponse(orderResponse.responseType, message)

        fetchPeriodic()

        return OrderResponse(
            id = id,
            responseType = responseType,
            message = message
        )
    }
    suspend fun postMarketOrder(
        quantity: Double,
        asset: Asset,
        isNotionalOrder: Boolean = false
    ): OrderResponse {
        val orderResponse: BasicResponse<Map<String, Any>> = /*if (isNotionalOrder) {
        gedikDriveWealthOrderApiProvider.postNotionalMarketOrder(
            quantity,
            asset.symbol
        )
    } else {*/
            orderAPIProvider.postMarketOrder(quantity, asset.symbol)
        //}

        return handleOrderResponse(orderResponse)
    }

    suspend fun postLimitOrder(
        quantity: Int,
        limitPrice: Double,
        asset: Asset
    ): OrderResponse {
        val orderResponse: BasicResponse<Map<String, Any>> = orderAPIProvider.postLimitOrder(quantity, asset.symbol, limitPrice)
        return handleOrderResponse(orderResponse)
    }

    suspend fun cancelOrder(orderId: String): DeleteOrderResponse {
        val orderResponse: DeleteOrderResponse = orderAPIProvider.deleteOrder(orderId)

        val responseType: DeleteOrderResponseTypes = orderResponse.responseType
        val message: String? = orderResponse.message

        fetchPeriodic()

        return DeleteOrderResponse(
            responseType = responseType,
            message = message
        )
    }

    suspend fun improveOrder(orderId: String, symbol: String, newPrice: Double, newQuantity: Int): OrderResponse {
        val orderResponse: BasicResponse<Map<String, Any>> = orderAPIProvider.putImproveOrder(orderId, symbol, newPrice, newQuantity)

        return handleOrderResponse(orderResponse)
    }

    suspend fun fetchPeriodic() {
        delay(2000L)
        // You can uncomment the below code if you need to run these in parallel.
        /*
        coroutineScope {
            launch { portfolioHandler.fetchUserStockData() }
            launch { portfolioHandler.fetchUserEquityData() }
            launch { portfolioHandler.fetchTransactionsPeriodic() }
        }
        */
    }
}

data class OrderResponse(
    val id:OrderId,
    val responseType: OrderStatus,
    val message: String? = null
)
