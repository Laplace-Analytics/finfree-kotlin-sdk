package sdk.trade

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sdk.base.logger
import sdk.models.core.AssetProvider
import sdk.trade.repositories.repos.OrdersRepository
import java.time.LocalDateTime

class OrderUpdatesHandler(
    private val orderUpdatesListener: OrderUpdatesListener,
    private val ordersDataHandler: OrdersDataHandler,
    private val ordersRepository: OrdersRepository,
    private val assetProvider: AssetProvider
) {
    private val orderStream = BehaviorSubject.create<OrderData>()
    private val _orderSequenceNumbers = mutableMapOf<OrderId, Int>()
    private var _subscription: Disposable

    init {
        _subscription = orderUpdatesListener.listen(onData = { value ->
            try {

                val type = object : TypeToken<MutableMap<String, Any>>() {}.type

                val rawData: MutableMap<String, Any> = Gson().fromJson(value,type)
                val orderId:OrderId = OrderId.fromValue(
                    rawData.get("order_number")
                        ?: rawData.get("dw_order_id")
                        ?: rawData.get("order_id")!!
                )

                val sequenceNo = rawData.get("sequence_number") ?: rawData.get("order_id")
                if ((_orderSequenceNumbers[orderId] ?: -1) <= sequenceNo as Int) {
                    _orderSequenceNumbers[orderId] = sequenceNo
                    logger.info("Update for order $orderId\n$rawData")

                    val orderData = getOrderData(rawData)
                    CoroutineScope(Dispatchers.IO).launch {
                        if(orderData != null){
                            ordersDataHandler.updateOrder(orderData, !realTradeNonFinalOrderStatus.contains(orderData.status))
                            orderStream.onNext(orderData)
                        }else{
                            logger.error("RealOrderData from websocket could not be constructed: $rawData")
                        }
                    }

                }
            } catch (ex: Exception) {
                logger.error("Could not update the order data: $value\n $ex")
            }
        },
        )
    }

    private fun getOrderData(rawData: MutableMap<String, Any>): OrderData? {
        val orderDateString:String? = rawData["order_date"] as String?  ?: rawData["last_updated"] as String?
        if (orderDateString != null && !orderDateString.endsWith("Z")) {
            rawData["order_date"] = LocalDateTime.parse(orderDateString).toString()
        } else {
            rawData["order_date"] = orderDateString.toString()
        }

        val recordDateString = rawData["record_date"] as? String
            ?: rawData["placed_date"] as? String
            ?: rawData["created_at"] as? String

        if (recordDateString != null && !recordDateString.endsWith("Z")) {
            rawData["record_date"] = LocalDateTime.parse(recordDateString).toString()
        } else {
            rawData["record_date"] = recordDateString.toString()
        }

        return OrderData.fromJson(rawData, assetProvider)
    }

    fun close() {
        _subscription?.dispose()
    }
}
