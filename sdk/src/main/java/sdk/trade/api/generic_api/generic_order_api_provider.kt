package sdk.trade

import kotlinx.coroutines.flow.Flow
import sdk.base.network.*
import sdk.models.data.assets.Asset

abstract class GenericOrderAPIProvider(
    override val httpHandler: HTTPHandler,
    open val basePath: String
) : GenericApiProvider(httpHandler) {

    abstract suspend fun postLimitOrder(quantity: Int, asset: Asset, limitPrice: Double): BasicResponse<Map<String, Any>>

    abstract suspend fun postMarketOrder(quantity: Number, asset: Asset): BasicResponse<Map<String, Any>>

    abstract suspend fun putImproveOrder(orderId: OrderId, asset: Asset, newPrice: Double, newQuantity: Int): BasicResponse<Map<String, Any>>

    abstract suspend fun deleteOrder(orderId: OrderId): DeleteOrderResponse

    abstract suspend fun getOrder(orderId: String): BasicResponse<Map<String, Any>>

    /*
    // Uncommented the function if you need to implement it
    suspend fun getOrders(
        statuses: List<OrderStatus>? = null,
        from: Int? = null,
        to: Int? = null,
        limit: Int? = null,
        startDate: DateTime? = null,
        endDate: DateTime? = null
    ): BasicResponse<List<Map<String, Any>>> {
        val params = mutableMapOf<String, String>().apply {
            statuses?.takeIf { it.isNotEmpty() }?.let { put("statuses", it.toString()) }
            from?.let { put("from", to.toString()) }
            to?.let { put("to", it.toString()) }
            limit?.let { put("limit", it.toString()) }
            startDate?.let { put("startDate", it.toString()) }
            endDate?.let { put("endDate", it.toString()) }
        }

        val response = httpHandler.get(path = basePath, data = params)
        return ApiResponseHandler.handleResponse<List<Map<String, Any>>>(
            response,
            onSuccess = { response ->
                val data = json.decode(response.body)
                BasicResponse<List<Map<String, Any>>>(
                    responseType = BasicResponseTypes.success,
                    data = data.map { it.toMap() }
                )
            },
            onNoContent = { BasicResponse<List<Map<String, Any>>>(
                responseType = BasicResponseTypes.success,
                data = emptyList()
            )},
            onError = { BasicResponse<List<Map<String, Any>>>(
                responseType = BasicResponseTypes.error
            )}
        )
    }
    */

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




