package sdk.trade.models.order

import io.reactivex.disposables.Disposable

abstract class GenericOrderUpdatesListener {
    abstract suspend fun openConnection(token: String)
    abstract fun closeChannel()
    abstract fun listen(
        onData: (value: String) -> Unit,
        onError: ((Throwable) -> Unit)? = null,
        onDone: (() -> Unit)? = null,
    ): Disposable
}
