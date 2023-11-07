package sdk.trade.repositories.drivewealth_repos

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import sdk.base.logger
import sdk.base.network.BasicResponse
import sdk.base.network.BasicResponseTypes
import sdk.models.core.FinfreeSDK
import sdk.models.data.assets.Asset
import sdk.models.data.assets.PortfolioType
import sdk.trade.api.generic_api.DeleteOrderResponse
import sdk.trade.api.generic_api.DeleteOrderResponseTypes
import sdk.trade.api.generic_api.GenericOrderAPIProvider
import sdk.trade.OrderHandler
import sdk.trade.OrderId
import sdk.trade.OrderResponse
import sdk.trade.OrderStatus

class DriveWealthOrderHandler(
  override val orderAPIProvider: GenericOrderAPIProvider
): OrderHandler(orderAPIProvider = orderAPIProvider){

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
        try {
            val id = OrderId.fromValue((orderResponse.data ?: emptyMap())["orderID"])
            val message = orderResponse.message
            val responseType = orderStatusFromResponse(orderResponse.responseType, message)

            fetchPeriodic()

            return OrderResponse(
                id = id,
                responseType = responseType,
                message = message
            )
        }catch (ex:Exception){
            logger.error("Order response couldn't be handled", ex)
            return OrderResponse(
                responseType = OrderStatus.ServerError,
            )
        }

    }
    override suspend fun postMarketOrder(
        quantity: Double,
        asset: Asset,
        isNotionalOrder: Boolean
    ): OrderResponse {
        val orderResponse: BasicResponse<Map<String, Any>> = /*if (isNotionalOrder) {
        gedikDriveWealthOrderApiProvider.postNotionalMarketOrder(
            quantity,
            asset.symbol
        )
    } else {*/
            orderAPIProvider.postMarketOrder(
                quantity,
                asset
            )
        //}

        return handleOrderResponse(orderResponse)
    }

    override suspend fun postLimitOrder(
        quantity: Int,
        limitPrice: Double,
        asset: Asset
    ): OrderResponse {
        val orderResponse: BasicResponse<Map<String, Any>> = orderAPIProvider.postLimitOrder(
            quantity,
            asset,
            limitPrice
        )
        return handleOrderResponse(orderResponse)
    }

    override suspend fun cancelOrder(orderId: OrderId): DeleteOrderResponse {
        val orderResponse: DeleteOrderResponse = orderAPIProvider.deleteOrder(orderId)

        val responseType: DeleteOrderResponseTypes = orderResponse.responseType
        val message: String? = orderResponse.message

        fetchPeriodic()

        return DeleteOrderResponse(
            responseType = responseType,
            message = message
        )
    }

    override suspend fun improveOrder(
        orderId: OrderId,
        asset: Asset,
        newPrice: Double,
        newQuantity: Int
    ): OrderResponse {
        val orderResponse: BasicResponse<Map<String, Any>> = orderAPIProvider.putImproveOrder(
            orderId,
            asset,
            newPrice,
            newQuantity
        )

        return handleOrderResponse(orderResponse)
    }

    override suspend fun fetchPeriodic() {
        delay(2000L)

        coroutineScope {
            async { FinfreeSDK.portfolioHandler(PortfolioType.DriveWealth).fetchUserPortfolio() }.await()
            async { FinfreeSDK.portfolioHandler(PortfolioType.DriveWealth).fetchUserEquityData() }.await()
            async { FinfreeSDK.portfolioHandler(PortfolioType.DriveWealth).fetchTransactionsPeriodic() }.await()
        }
    }
}