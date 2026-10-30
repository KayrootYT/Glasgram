package com.glasgram.app.data.telegram

import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.drinkless.tdlib.Client
import com.glasgram.app.data.security.TdlibDatabaseKey
import org.drinkless.tdlib.TdApi
import java.util.concurrent.atomic.AtomicReference

/**
 * TDLib-backed Telegram session. API credentials are supplied at build time and are
 * never hard-coded into the repository.
 *
 * Every update is delivered (in order, on the TDLib thread) to [onUpdate]. Updates that
 * arrive before the gateway object exists are queued, so the first authorization state is never lost.
 */
class TdLibGateway private constructor(
    private val client: Client,
    private val context: Context,
    private val apiId: Int,
    private val apiHash: String,
    private val onUpdate: (TdLibGateway, TdApi.Object) -> Unit,
    private val onError: (String) -> Unit
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
            if (result is TdApi.Error) {
                val message = "Telegram error ${result.code}: ${result.message}"
                errorBus.tryEmit(message)
                onError(message)
            }
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

    private fun dispatch(update: TdApi.Object) {
        updateBus.tryEmit(update)
        if (update is TdApi.UpdateAuthorizationState) {
            handleAuthorizationState(update.authorizationState)
        }
        onUpdate(this, update)
    }

    private fun handleAuthorizationState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val base = context.filesDir.absolutePath + "/glasgram-tdlib"
                try {
                    val databaseKey = TdlibDatabaseKey.getOrCreate(context)
                    send(
                        TdApi.SetTdlibParameters(
                            false,
                            "$base/database",
                            "$base/files",
                            databaseKey,
                            true,
                            true,
                            true,
                            false,
                            apiId,
                            apiHash,
                            "en",
                            Build.MODEL ?: "Android",
                            Build.VERSION.RELEASE ?: "Android",
                            com.glasgram.app.BuildConfig.VERSION_NAME
                        )
                    )
                } catch (error: Exception) {
                    val message = "Could not unlock the encrypted Telegram database: ${error.message ?: error.javaClass.simpleName}"
                    errorBus.tryEmit(message)
                    onError(message)
                }
            }
            else -> Unit
        }
    }

    companion object {
        fun create(
            context: Context,
            apiId: Int,
            apiHash: String,
            onUpdate: (TdLibGateway, TdApi.Object) -> Unit = { _, _ -> },
            onError: (String) -> Unit = {}
        ): TdLibGateway {
            require(apiId > 0) { "Telegram API ID must be a positive integer." }
            require(apiHash.isNotBlank()) { "Telegram API hash is required." }

            val appContext = context.applicationContext
            // This TDLib Java binding does not load its JNI libraries itself.
            // Load OpenSSL dependencies first, then the TDLib JNI bridge.
            try {
                System.loadLibrary("cryptox")
                System.loadLibrary("sslx")
                System.loadLibrary("tdjni")
            } catch (error: UnsatisfiedLinkError) {
                throw IllegalStateException(
                    "Could not load TDLib native libraries for this device ABI: ${error.message}",
                    error
                )
            }

            val lock = Any()
            val ref = AtomicReference<TdLibGateway?>(null)
            val pending = java.util.ArrayDeque<TdApi.Object>()

            val exceptionHandler = Client.ExceptionHandler { error ->
                ref.get()?.errorBus?.tryEmit(error.message ?: "TDLib exception")
            }

            val client = Client.create(
                Client.ResultHandler { update ->
                    val ready = synchronized(lock) {
                        val current = ref.get()
                        if (current == null) pending.add(update)
                        current
                    }
                    ready?.dispatch(update)
                },
                exceptionHandler,
                exceptionHandler
            )

            val gateway = TdLibGateway(client, appContext, apiId, apiHash, onUpdate, onError)
            val backlog = synchronized(lock) {
                ref.set(gateway)
                val copy = pending.toList()
                pending.clear()
                copy
            }
            backlog.forEach { gateway.dispatch(it) }
            return gateway
        }
    }
}
