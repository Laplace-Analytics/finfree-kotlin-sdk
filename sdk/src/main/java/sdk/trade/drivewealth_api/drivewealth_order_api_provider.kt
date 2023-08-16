package sdk.trade

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*
import java.util.stream.Stream
import kotlin.math.absoluteValue

class DriveWealthOrderAPIProvider(
    override val httpHandler: HTTPHandler,
    override val basePath: String

): GenericOrderAPIProvider(httpHandler,basePath){
    override suspend fun postLimitOrder(
        quantity: Int,
        symbol: String,
        limitPrice: Double
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "LIMIT",
            "price" to  limitPrice,
            "symbol" to  symbol,
            "side" to if (quantity > 0) "BUY" else "SELL",
            "quantity" to quantity.absoluteValue.toString()
        )

        val response = httpHandler.post(path = basePath, body = Json.encodeToString(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data:Map<String,Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data,
                    message = null
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    override suspend fun postMarketOrder(
        quantity: Number,
        symbol: String
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "MARKET",
            "symbol" to  symbol,
            "side" to if (quantity.toDouble() > 0) "BUY" else "SELL",
            "quantity" to when {
                quantity is Int -> quantity.absoluteValue.toString()
                quantity is Double -> String.format("%.3f", quantity.absoluteValue)
                else -> throw IllegalArgumentException("Unsupported type for quantity")
            }
        )

        val response = httpHandler.post(path = basePath, body = Json.encodeToString(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data:Map<String,Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data,
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }

    override suspend fun putImproveOrder(
        orderId: String,
        symbol: String,
        newPrice: Double,
        newQuantity: Int
    ): BasicResponse<Map<String, Any>> {
        throw NotImplementedError("Not implemented yet")
    }

    override suspend fun deleteOrder(orderId: String): DeleteOrderResponse {
        val response = httpHandler.delete(path = "$basePath/$orderId")

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                DeleteOrderResponse(
                    responseType = DeleteOrderResponseTypes.Success,
                    message = res.body?.string(),
                )
            },
            onError = { res ->
                DeleteOrderResponse(
                    responseType = DeleteOrderResponseTypes.NotFound,
                    message = res.body?.string()
                )
            },
            onBadRequest = { res ->
                DeleteOrderResponse(
                    responseType = DeleteOrderResponseTypes.UnknownError,
                    message = res.body?.string()
                )

                
            }
        ) as DeleteOrderResponse
    }

    override suspend fun getOrder(orderId: String): BasicResponse<Map<String, Any>> {
        val response = httpHandler.get(path = "$basePath/$orderId")

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data:Map<String,Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data,
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<Map<String, Any>>

    }

    override suspend fun getTransactionsBetween(
        from: Int,
        to: Int
    ): BasicResponse<List<Map<String, Any>>> {
        val response = httpHandler.get(
            path = basePath,
            data = mapOf(
                "from" to from.toString(),
                "to" to to.toString()
            )
            )

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data:List<Map<String,Any>>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data,
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    override suspend fun listenOrders(streamUUID: String): BasicResponse<Stream<StreamData>> {

    }


    suspend fun postLimitOrder(
        amount: Double,
        symbol: String,
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "MARKET",
            "symbol" to  symbol,
            "side" to if (amount > 0) "BUY" else "SELL",
            "amountCash" to amount.absoluteValue.toString()
        )

        val response = httpHandler.post(path = basePath, body = Json.encodeToString(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val data:Map<String,Any>  = Json.decodeFromString(res.body!!.string())
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = data,
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }



}