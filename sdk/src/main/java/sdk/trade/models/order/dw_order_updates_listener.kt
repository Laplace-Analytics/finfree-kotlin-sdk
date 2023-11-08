package sdk.trade.models.order

import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sdk.trade.api.generic_api.GenericOrderAPIProvider


class DrivewealthOrderUpdatesListener(
    private val orderApiProvider: GenericOrderAPIProvider
): GenericOrderUpdatesListener() {

    private val stream = BehaviorSubject.create<String>()
    private var subscription: Disposable? = null

    override suspend fun openConnection(token: String) {
        val response = orderApiProvider.listenOrders(UUID.randomUUID().toString())
        val responseFlowable = response.data?.asFlowable()

        if (response.responseType != BasicResponseTypes.Success || response.data == null) {
            delay(60000)  // wait for 60 seconds
            logger.info("Trying to open websocket connection again!")
            openConnection(token)
            delay(1000)
            return
        }

        subscription = responseFlowable?.subscribe(
                { event ->
                    try {
                        val data = event.data
                        val decodedData:Any = Json.decodeFromString(data)
                        if (decodedData is String && decodedData == "{\"hearbeat\": \"hearbeat\"}") {
                            return@subscribe
                        }
                        stream.onNext(data)
                    } catch (ex: Exception) {
                        logger.error("Error while decoding websocket data", ex)
                    }
                },
                { error ->
                    stream.onError(error)
                    subscription?.dispose()
                },
                {
                    CoroutineScope(Dispatchers.IO).launch{
                        closeChannel()
                        delay(15000) // 15 seconds delay
                        logger.info("Trying to open websocket connection again!")
                        openConnection(token)
                        delay(1000)
                    }
                }
            )
    }
    override fun closeChannel() {
        stream.onComplete()
        subscription?.dispose()
    }

    override fun listen(
        onData: (value: String) -> Unit,
        onError: ((Throwable) -> Unit)?,
        onDone: (() -> Unit)?
    ): Disposable {
        return stream.subscribe(
            onData,
            onError,
            onDone
        )
    }
}