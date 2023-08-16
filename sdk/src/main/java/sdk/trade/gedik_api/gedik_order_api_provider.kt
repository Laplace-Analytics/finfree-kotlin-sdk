package sdk.trade.gedik_api

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.logger
import sdk.base.network.*
import sdk.trade.DeleteOrderResponse
import sdk.trade.DeleteOrderResponseTypes
import sdk.trade.OrderStatus

class GedikOrderApiProvider(
    override val httpHandler: HTTPHandler,
    private val basePath: String
) : GenericApiProvider(httpHandler) {

    private var testAPI: Boolean = false

    val pathTestExtension: String
        get() = if (testAPI) "test" else ""

    fun enableTestAPI() {
        testAPI = true
    }

    suspend fun getOrders() : BasicResponse<List<Map<String, Any>>> {
        val path: String = "execute$pathTestExtension/orders"
        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val body = Json.decodeFromString<List<Map<String, Any>>>((res.body!!.string()))
                BasicResponse(
                    data = body,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    suspend fun getOrder(orderId:String) : BasicResponse<Map<String, Any>> {
        val path: String = "execute$pathTestExtension/order/$orderId"
        val response = httpHandler.get(path = path, tryAgainOnTimeout = true)
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val body = Json.decodeFromString<Map<String, Any>>((res.body!!.string()))
                BasicResponse(
                    data = body,
                    responseType = BasicResponseTypes.Success,
                    message = null
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    suspend fun postLimitOrder(quantity:Int,symbol:String,period:String,limitPrice:Double,warningAccepted:Boolean? = null) : RealOrderResponse {
        val path: String = "stock$pathTestExtension/order"
        val body = mapOf(
            "price" to limitPrice,
            "quantity" to quantity,
            "symbol" to symbol,
            "order_period" to period,
            "order_type" to "2",
            warningAccepted.let { "warning_accepted" to it }
        )
        val response = httpHandler.post(path = path, body = Json.encodeToString(body))
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val responseBody = res.body!!.string()
                val data = Json.decodeFromString<Map<String, Any>>(responseBody)
                try {
                     RealOrderResponse(
                        data =  data.get("order_id").toString(),
                        responseType =  getGedikOrderStatusFromStatusCode(data["status"] as Int),
                        message =  responseBody,
                    )


                }catch (ex:Exception){
                    logger.error("Error while processing order error response\n$ex")
                    RealOrderResponse(message =  responseBody, responseType =  OrderStatus.UnspecifiedOrderStatus)

                }
            },

            onError = { res ->
                val responseBody = res.body.toString()
                try {
                    val data: Map<String, Any> = Json.decodeFromString(responseBody)
                    RealOrderResponse(
                        message = responseBody,
                        responseType = getGedikOrderStatusFromStatusCode(data["status"] as Int)
                    )
                } catch (ex: Exception) {
                    logger.error("Error while processing order error response\n$ex")
                    RealOrderResponse(
                        message = responseBody,
                        responseType = OrderStatus.UnspecifiedOrderStatus
                    )
                }
            },
            onConnectionClosedWithoutResponse = { res ->
                RealOrderResponse(
                    message = "Connection closed without a response.",
                    responseType = OrderStatus.UnspecifiedOrderStatus
                )
            }

        ) as RealOrderResponse
    }

    suspend fun postMarketOrder(quantity:Int,symbol:String,period:String,warningAccepted:Boolean? = null) : RealOrderResponse {
        val path: String = "stock$pathTestExtension/order"
        val body = mapOf(
            "quantity" to quantity,
            "symbol" to symbol,
            "order_period" to period,
            "order_type" to "1",
            warningAccepted.let { "warning_accepted" to it }
        )
        val response = httpHandler.post(path = path, body = Json.encodeToString(body))
        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val responseBody = res.body!!.string()
                val data = Json.decodeFromString<Map<String, Any>>(responseBody)
                try {
                    RealOrderResponse(
                        data =  data.get("order_id").toString(),
                        responseType =  getGedikOrderStatusFromStatusCode(data["status"] as Int),
                        message =  responseBody,
                    )


                }catch (ex:Exception){
                    logger.error("Error while processing order error response\n$ex")
                    RealOrderResponse(message =  responseBody, responseType =  OrderStatus.UnspecifiedOrderStatus)

                }
            },

            onError = { res ->
                val responseBody = res.body!!.string()
                try {
                    val data: Map<String, Any> = Json.decodeFromString(responseBody)
                    RealOrderResponse(
                        message = responseBody,
                        responseType = getGedikOrderStatusFromStatusCode(data["status"] as Int)
                    )
                } catch (ex: Exception) {
                    logger.error("Error while processing order error response\n$ex")
                    RealOrderResponse(
                        message = responseBody,
                        responseType = OrderStatus.UnspecifiedOrderStatus
                    )
                }
            },
            onConnectionClosedWithoutResponse = { res ->
                RealOrderResponse(
                    message = "Connection closed without a response.",
                    responseType = OrderStatus.UnspecifiedOrderStatus
                )
            }

        ) as RealOrderResponse
    }

    suspend fun deleteOrder(orderId: String): DeleteOrderResponse {
        val path = "stock$pathTestExtension/order/$orderId"

        val response = httpHandler.delete(path = path)
        return ApiResponseHandler.handleResponse(
            response = response,
            onSuccess = { res -> DeleteOrderResponse(responseType = DeleteOrderResponseTypes.Success, message = res.body?.string()) },
            onNotFound = { res -> DeleteOrderResponse(responseType = DeleteOrderResponseTypes.NotFound, message = res.body?.string()) },
            onError = { res -> DeleteOrderResponse(responseType = DeleteOrderResponseTypes.UnknownError, message = res.body?.string()) }
        ) as DeleteOrderResponse
    }





/// will be contiune

}

fun getGedikOrderStatusFromStatusCode(statusCode: Int): OrderStatus {
    return gedikOrderStatusCodes[statusCode] ?: OrderStatus.UnspecifiedOrderStatus
}

val gedikOrderStatusCodes = mapOf(
    -38 to OrderStatus.MarketOrderDidNotExecute,
    -18 to OrderStatus.SentCancelRequestToExchange,
    -17 to OrderStatus.SendingCancelRequestToExchange,
    -16 to OrderStatus.StockIsNotTraded,
    -15 to OrderStatus.OldOrder,
    -11 to OrderStatus.IncorrectCorrection,
    -10 to OrderStatus.OrderCancelled,
    -9 to OrderStatus.OrderExecuted,
    -8 to OrderStatus.OrderPeriodOver,
    -7 to OrderStatus.IncorrectOrder,
    -5 to OrderStatus.InsufficientBalance,
    -4 to OrderStatus.OutOfLimit,
    -2 to OrderStatus.MainOrderIsBeingCorrected,
    -1 to OrderStatus.OrderIsBeingCorrected,
    0 to OrderStatus.TransmittingToExchange,
    1 to OrderStatus.TransmittedToExchange,
    4 to OrderStatus.OutOfLimit,
    5 to OrderStatus.InsufficientBalance,
    7 to OrderStatus.IncorrectOrder,
    8 to OrderStatus.OrderPeriodOver,
    9 to OrderStatus.OrderExecuted,
    10 to OrderStatus.OrderCancelled,
    11 to OrderStatus.OrderCancellationRefused
)

data class RealOrderResponse(
    override val data: String? = null,
    override val responseType: OrderStatus,
    override val message: String? = null
): ApiResponseObject<String,OrderStatus>








