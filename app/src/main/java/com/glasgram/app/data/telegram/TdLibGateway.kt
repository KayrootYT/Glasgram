package com.glasgram.app.data.telegram

import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicReference

/**
 * TDLib-backed Telegram session. API credentials are supplied by the user and are
 * never hard-coded into the repository. Authorization updates are exposed to UI.
 */
class TdLibGateway private constructor(
    private val client: Client,
    private val context: Context,
    private val apiId: Int,
    private val apiHash: String
) {
    private val updateBus = MutableSharedFlow<TdApi.Object>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val errorBus = MutableSharedFlow<String>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val updates: SharedFlow<TdApi.Object> = updateBus
    val errors: SharedFlow<String> = errorBus

    fun send(query: TdApi.Function<*>, callback: (TdApi.Object) -> Unit = {}) {
        client.send(query, Client.ResultHandler { result ->
            if (result is TdApi.Error) errorBus.tryEmit("Telegram error ${result.code}: ${result.message}")
            callback(result)
        })
    }

    fun submitPhoneNumber(phoneNumber: String) {
        send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, TdApi.PhoneNumberAuthenticationSettings()))
    }

    fun submitCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code))
    }

    fun submitPassword(password: String) {
        send(TdApi.CheckAuthenticationPassword(password))
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val base = context.filesDir.absolutePath + "/glasgram-tdlib"
                send(
                    TdApi.SetTdlibParameters(
                        false,
                        "$base/database",
                        "$base/files",
                        ByteArray(0),
                        true,
                        true,
                        apiId,
                        apiHash,
                        "en",
                        Build.MODEL ?: "Android",
                        Build.VERSION.RELEASE ?: "Android",
                        "0.1.0"
                    )
                )
            }
        }
    }

    companion object {
        fun create(context: Context, apiId: Int, apiHash: String): TdLibGateway {
            require(apiId > 0) { "Telegram API ID must be a positive integer." }
            require(apiHash.isNotBlank()) { "Telegram API hash is required." }

            val appContext = context.applicationContext
            val gatewayRef = AtomicReference<TdLibGateway?>(null)
            val client = Client.create(
                Client.ResultHandler { update ->
                    gatewayRef.get()?.let { gateway ->
                        gateway.updateBus.tryEmit(update)
                        if (update is TdApi.UpdateAuthorizationState) {
                            gateway.handleAuthorizationState(update.authorizationState)
                        }
                    }
                },
                Client.ExceptionHandler { error ->
                    gatewayRef.get()?.errorBus?.tryEmit(error.message ?: "TDLib exception")
                },
                Client.ExceptionHandler { error ->
                    gatewayRef.get()?.errorBus?.tryEmit(error.message ?: "TDLib exception")
                }
            )
            return TdLibGateway(client, appContext, apiId, apiHash).also(gatewayRef::set)
        }
    }
}
