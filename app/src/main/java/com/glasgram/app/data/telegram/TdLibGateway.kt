package com.glasgram.app.data.telegram

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/**
 * Thin boundary around TDLib's asynchronous Java client.
 *
 * This gateway deliberately does not claim that Telegram is connected. A matching
 * libtdjni.so and the complete TDLib authorization state machine are prerequisites.
 */
class TdLibGateway private constructor(
    private val client: Client
) {
    private val _updates = MutableSharedFlow<TdApi.Object>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val updates: SharedFlow<TdApi.Object> = _updates

    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errors: SharedFlow<String> = _errors

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit = {}) {
        client.send(query, Client.ResultHandler { result -> callback(result) })
    }

    fun requestAuthorizationState(callback: (TdApi.Object) -> Unit = {}) {
        send(TdApi.GetAuthorizationState(), callback)
    }

    companion object {
        /**
         * Returns null if the native TDLib runtime is not packaged or fails to load.
         * Call from an appropriate lifecycle owner and report the failure to the UI.
         */
        fun createOrNull(): TdLibGateway? = try {
            lateinit var gateway: TdLibGateway
            val client = Client.create(
                Client.ResultHandler { update -> gateway._updates.tryEmit(update) },
                Client.ExceptionHandler { error -> gateway._errors.tryEmit(error.message ?: "TDLib error") },
                Client.ExceptionHandler { error -> gateway._errors.tryEmit(error.message ?: "TDLib error") }
            )
            gateway = TdLibGateway(client)
            gateway
        } catch (_: UnsatisfiedLinkError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
