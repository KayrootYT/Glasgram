package com.glasgram.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** A SharedPreferences-backed value that is also observable by Compose. */
class Pref<T : Any>(
    private val sp: SharedPreferences,
    private val key: String,
    default: T
) : ReadWriteProperty<Any?, T> {
    private val state = mutableStateOf(read(default))

    @Suppress("UNCHECKED_CAST")
    private fun read(d: T): T = when (d) {
        is Boolean -> sp.getBoolean(key, d) as T
        is Float -> sp.getFloat(key, d) as T
        is Int -> sp.getInt(key, d) as T
        else -> d
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state.value = value
        val editor = sp.edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Int -> editor.putInt(key, value)
            else -> Unit
        }
        editor.apply()
    }
}

/** All "Glasgram Settings": mod features Telegram itself does not offer. */
class GlasSettings(context: Context) {
    private val sp = context.getSharedPreferences("glasgram_settings", Context.MODE_PRIVATE)

    // Ghost & privacy
    var ghostRead by Pref(sp, "ghostRead", false)
    var alwaysOffline by Pref(sp, "alwaysOffline", false)

    // Message guard
    var keepDeleted by Pref(sp, "keepDeleted", true)
    var keepEdits by Pref(sp, "keepEdits", true)

    // Appearance
    var accentIndex by Pref(sp, "accentIndex", 0)
    var glassIntensity by Pref(sp, "glassIntensity", 0.55f)
    var bubbleRadius by Pref(sp, "bubbleRadius", 22f)
    var textScale by Pref(sp, "textScale", 1f)
    var animatedBackground by Pref(sp, "animatedBackground", true)

    // Interface
    var compactList by Pref(sp, "compactList", false)
    var use24h by Pref(sp, "use24h", true)
    var showIds by Pref(sp, "showIds", false)
    var doubleTapReply by Pref(sp, "doubleTapReply", true)
    var haptics by Pref(sp, "haptics", true)
}
