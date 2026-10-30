package com.glasgram.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Creates and persists a random TDLib database key encrypted by an Android Keystore AES key.
 * The raw TDLib key is never written to disk or SharedPreferences in plaintext.
 *
 * If the wrapped key cannot be decrypted, this fails closed. It never silently generates a
 * replacement key for an existing database, which would make the existing TDLib database unreadable.
 */
object TdlibDatabaseKey {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "glasgram_tdlib_wrapping_key_v1"
    private const val PREFS = "glasgram_secure_session"
    private const val WRAPPED_KEY = "tdlib_database_key_v1"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private const val KEY_LENGTH_BYTES = 32

    @Synchronized
    fun getOrCreate(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(WRAPPED_KEY, null)
        val wrappingKey = getOrCreateWrappingKey()

        if (stored != null) {
            val packed = Base64.decode(stored, Base64.NO_WRAP)
            require(packed.size > IV_LENGTH) { "Stored TDLib key is damaged. Clear app data only if you intend to reset the local session." }
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val ciphertext = packed.copyOfRange(IV_LENGTH, packed.size)
            return Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_BITS, iv))
                doFinal(ciphertext)
            }.also { require(it.size == KEY_LENGTH_BYTES) { "Invalid TDLib database key length." } }
        }

        val raw = ByteArray(KEY_LENGTH_BYTES).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val encrypted = cipher.doFinal(raw)
        val packed = cipher.iv + encrypted
        val saved = prefs.edit().putString(WRAPPED_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).commit()
        if (!saved) {
            raw.fill(0)
            throw IllegalStateException("Could not persist the encrypted TDLib database key.")
        }
        return raw
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
