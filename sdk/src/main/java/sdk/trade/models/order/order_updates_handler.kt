package sdk.trade.models.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sdk.base.logger
import sdk.models.core.AssetProvider
import sdk.trade.OrderData
import sdk.trade.OrderId
import sdk.trade.OrderUpdatesListener
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
                val orderId: OrderId = OrderId.fromValue(
                    rawData["order_number"]
                        ?: rawData["dw_order_id"]
                        ?: rawData["order_id"]!!
                )

                val sequenceNo = rawData["sequence_number"] ?: rawData["order_id"]
                if ((_orderSequenceNumbers[orderId] ?: -1) <= sequenceNo as Int) {
                    _orderSequenceNumbers[orderId] = sequenceNo
                    logger.info("Update for order $orderId\n$rawData")

                    val orderData = getOrderData(rawData)
                    CoroutineScope(Dispatchers.IO).launch {
                        ordersDataHandler.updateOrder(orderData, !realTradeNonFinalOrderStatus.contains(orderData.status))
                        orderStream.onNext(orderData)
                    }

                }
            } catch (ex: Exception) {
                logger.error("Could not update the order data: $value\n $ex")
            }
        },
        )
    }

    private fun getOrderData(rawData: MutableMap<String, Any>): OrderData {
        val orderDateString:String? = rawData["order_date"] as String?  ?: rawData["last_updated"] as String?
        if (orderDateString != null && !orderDateString.endsWith("Z")) {
            rawData["executed_date"] = LocalDateTime.parse(orderDateString).toString()
        } else {
            rawData["executed_date"] = orderDateString.toString()
        }

        val recordDateString = rawData["record_date"] as String?
            ?: rawData["placed_date"] as String?
            ?: rawData["created_at"] as String

        if (!recordDateString.endsWith("Z")) {
            rawData["placed_date"] = LocalDateTime.parse(recordDateString).toString()
        } else {
            rawData["placed_date"] = recordDateString
        }

        return ordersRepository.orderDataFromJSON(rawData)
    }

    fun close() {
        _subscription.dispose()
    }
}
