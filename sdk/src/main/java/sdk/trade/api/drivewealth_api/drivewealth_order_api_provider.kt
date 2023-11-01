package sdk.trade.api.drivewealth_api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sdk.base.network.*
import sdk.models.Asset
import sdk.models.AssetSymbol
import sdk.trade.DeleteOrderResponse
import sdk.trade.DeleteOrderResponseTypes
import sdk.trade.GenericOrderAPIProvider
import sdk.trade.OrderId
import java.util.Locale
import kotlin.math.absoluteValue

class DriveWealthOrderAPIProvider(
    override val httpHandler: HTTPHandler,
    override val basePath: String

): GenericOrderAPIProvider(httpHandler,basePath){
    override suspend fun postLimitOrder(
        quantity: Int,
        asset: Asset,
        limitPrice: Double
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "LIMIT",
            "price" to  limitPrice,
            "symbol" to  asset.symbol,
            "side" to if (quantity > 0) "BUY" else "SELL",
            "quantity" to quantity.absoluteValue.toString()
        )

        val response = httpHandler.post(path = basePath, body = Gson().toJson(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
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
        asset: Asset
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "MARKET",
            "symbol" to  asset.symbol,
            "side" to if (quantity.toDouble() > 0) "BUY" else "SELL",
            "quantity" to when {
                quantity is Int -> quantity.absoluteValue.toString()
                quantity is Double -> String.format(Locale.US,"%.3f", quantity.absoluteValue)
                else -> throw IllegalArgumentException("Unsupported type for quantity")
            }
        )

        val response = httpHandler.post(path = basePath, body = Gson().toJson(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
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
        orderId: OrderId,
        asset: Asset,
        newPrice: Double,
        newQuantity: Int
    ): BasicResponse<Map<String, Any>> {
        throw NotImplementedError("Not implemented yet")
    }

    override suspend fun deleteOrder(orderId: OrderId): DeleteOrderResponse {
        val response = httpHandler.delete(path = "$basePath/${orderId.id}")

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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
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

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<List<Map<String, Any>>>() {}.type

                val result: List<Map<String, Any>> = Gson().fromJson(responseBodyStr,type)

                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = result,
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<List<Map<String, Any>>>
    }

    override suspend fun listenOrders(streamUUID: String): BasicResponse<Flow<StreamData>> {
        val endpoint = "$basePath/listen"
        val response = httpHandler.getStreamedRequest(
            path = endpoint,
            data = mapOf("stream" to streamUUID)
        )

        return ApiResponseHandler.handleStreamedResponse<Flow<StreamData>>(
            response = response,
            onSuccess = { response ->
                var currentMessage = ""

                val transformedFlow = flow {
                    response.body?.byteStream()?.bufferedReader()?.use { reader ->
                        for (line in reader.lineSequence()) {
                            currentMessage += line
                            val currentLines = currentMessage.split("\n")
                            val idLine = currentLines.firstOrNull { it.startsWith("id: ") }
                            val dataLine = currentLines.firstOrNull { it.startsWith("data: ") }
                            val eventLine = currentLines.firstOrNull { it.startsWith("event: ") }

                            if (idLine != null && dataLine != null && eventLine != null) {
                                val current = "$idLine\n$dataLine\n$eventLine"
                                val streamData = StreamData(current)
                                currentMessage = ""
                                emit(streamData)
                            }
                        }
                    }
                }

                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = transformedFlow
                )
            },
            onError = {
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<Flow<StreamData>>
    }



    suspend fun postLimitOrder(
        amount: Double,
        symbol: AssetSymbol,
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "MARKET",
            "symbol" to  symbol,
            "side" to if (amount > 0) "BUY" else "SELL",
            "amountCash" to amount.absoluteValue
        )

        val response = httpHandler.post(path = basePath, body = Gson().toJson(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->

                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val result: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
                BasicResponse(
                    responseType = BasicResponseTypes.Success,
                    data = result,
                )
            },
            onError = { res ->
                BasicResponse(
                    responseType = BasicResponseTypes.Error
                )
            }
        ) as BasicResponse<Map<String, Any>>
    }
    suspend fun postNotionalMarketOrder(
        amount: Double,
        asset: Asset,
    ): BasicResponse<Map<String, Any>> {
        val params: Map<String,Any> = mapOf(
            "orderType" to  "MARKET",
            "symbol" to  asset.symbol,
            "side" to if (amount > 0) "BUY" else "SELL",
            "amountCash" to amount.absoluteValue
        )

        val response = httpHandler.post(path = basePath, body = Gson().toJson(params))

        return ApiResponseHandler.handleResponse(
            response,
            onSuccess = { res ->
                val responseBodyStr = res.body?.string() ?: ""
                val type = object : TypeToken<Map<String, Any>>() {}.type

                val data: Map<String, Any> = Gson().fromJson(responseBodyStr,type)
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
    fun unit8Transformer(input: Flow<ByteArray>): Flow<List<Int>> {
        return input.map { byteArray -> byteArray.map { it.toInt() } }
    }
}