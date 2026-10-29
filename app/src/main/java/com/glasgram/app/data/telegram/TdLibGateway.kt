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
    private val client: Client,
    private val updateBus: MutableSharedFlow<TdApi.Object>,
    private val errorBus: MutableSharedFlow<String>
) {
    val updates: SharedFlow<TdApi.Object> = updateBus
    val errors: SharedFlow<String> = errorBus

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
        fun createOrNull(): TdLibGateway? {
            val updateBus = MutableSharedFlow<TdApi.Object>(
                replay = 0,
                extraBufferCapacity = 128,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            val errorBus = MutableSharedFlow<String>(
                replay = 0,
                extraBufferCapacity = 32,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            return try {
                val client = Client.create(
                    Client.ResultHandler { update -> updateBus.tryEmit(update) },
                    Client.ExceptionHandler { error -> errorBus.tryEmit(error.message ?: "TDLib error") },
                    Client.ExceptionHandler { error -> errorBus.tryEmit(error.message ?: "TDLib error") }
                )
                TdLibGateway(client, updateBus, errorBus)
            } catch (_: UnsatisfiedLinkError) {
                null
            } catch (_: RuntimeException) {
                null
            }
        }
    }
}
