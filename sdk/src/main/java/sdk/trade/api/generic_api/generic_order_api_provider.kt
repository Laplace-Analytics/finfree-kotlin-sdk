package sdk.trade.api.generic_api

import kotlinx.coroutines.flow.Flow
import sdk.base.network.*
import sdk.models.data.assets.Asset
import sdk.trade.OrderId

abstract class GenericOrderAPIProvider(
    override val httpHandler: HTTPHandler,
    open val basePath: String
) : GenericApiProvider(httpHandler) {

    abstract suspend fun postLimitOrder(quantity: Int, asset: Asset, limitPrice: Double): BasicResponse<Map<String, Any>>

    abstract suspend fun postMarketOrder(quantity: Number, asset: Asset): BasicResponse<Map<String, Any>>

    abstract suspend fun putImproveOrder(orderId: OrderId, asset: Asset, newPrice: Double, newQuantity: Int): BasicResponse<Map<String, Any>>

    abstract suspend fun deleteOrder(orderId: OrderId): DeleteOrderResponse

    abstract suspend fun getOrder(orderId: String): BasicResponse<Map<String, Any>>

    abstract suspend fun getAllOrders(): BasicResponse<List<Map<String, Any>>>

    abstract suspend fun getTransactionsBetween(from: Int, to: Int): BasicResponse<List<Map<String, Any>>>

    abstract suspend fun listenOrders(streamUUID: String): BasicResponse<Flow<StreamData>>
}

enum class DeleteOrderResponseTypes {
    Success,
    NotFound,
    UnknownError,
}


data class DeleteOrderResponse(
    override val responseType: DeleteOrderResponseTypes,
    override val message: String? = null,
    override val data: Unit? = null
) : ApiResponseObject<Unit, DeleteOrderResponseTypes>




