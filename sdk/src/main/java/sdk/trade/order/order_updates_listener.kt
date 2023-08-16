package sdk.trade

import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import sdk.base.logger
import sdk.base.network.BasicResponseTypes
import sdk.base.network.StreamData
import java.util.*
import java.util.stream.Stream
import io.reactivex.Observable
import jdk.internal.org.jline.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


class OrderUpdatesListener(private val orderApiProvider: GenericOrderAPIProvider) {

    private val stream = BehaviorSubject.create<String>()
    private var subscription: Disposable? = null

    suspend fun openConnection(token: String) {
        val response = orderApiProvider.listenOrders(UUID.randomUUID().toString())
        val responseStream = Observable.create<StreamData> { emitter ->
            response.data?.forEach {
                emitter.onNext(it)
            } ?: emitter.onError(NullPointerException("No data"))
            emitter.onComplete()
        }
        if (response.responseType != BasicResponseTypes.Success || response.data == null) {
            delay(60000)  // wait for 60 seconds
            Log.info("Trying to open websocket connection again!")
            openConnection(token)
            delay(1000)
            return
        }

        subscription = responseStream
            .subscribe(
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
                        Log.info("Trying to open websocket connection again!")
                        openConnection(token)
                        delay(1000)
                    }
                }
            )
    }
    fun closeChannel() {
        stream.onComplete()
        subscription?.dispose()
    }

    fun listen(
        onData: (String) -> Unit,
        onError: ((Throwable) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
    ): Disposable {
        return stream.subscribe(
            onData,
            onError,
            onComplete,
        )
    }
}