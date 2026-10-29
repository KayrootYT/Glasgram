package com.glasgram.app.data.telegram

/**
 * Build-safe placeholder for the Telegram transport integration.
 *
 * The TDLib Java bindings and matching native libtdjni.so are not included yet.
 * This class intentionally does not report a live Telegram connection.
 * Follow docs/TDLIB_SETUP.md before replacing this with the real TDLib client.
 */
class TdLibGateway private constructor() {
    val isConfigured: Boolean = false

    fun statusMessage(): String =
        "Telegram connection is not configured yet. Add the matching TDLib Java bindings and native libraries."

    companion object {
        fun createOrNull(): TdLibGateway? = null
    }
}
