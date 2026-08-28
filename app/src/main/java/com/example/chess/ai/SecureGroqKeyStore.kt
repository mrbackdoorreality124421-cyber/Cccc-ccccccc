package com.example.chess.ai

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/** Stores the user's Groq key encrypted with an Android Keystore AES key. */
class SecureGroqKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("groq_secure", Context.MODE_PRIVATE)
    private val alias = "chess_groq_api_key"

    private fun key(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        return try {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (ks.getKey(alias, null) as? SecretKey) ?: createKey(generator)
        } catch (_: Exception) { createKey(generator) }
    }

    private fun createKey(generator: KeyGenerator): SecretKey {
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        return generator.generateKey()
    }

    fun save(value: String) {
        if (value.isBlank()) { clear(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val blob = cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        prefs.edit().putString("key", Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    fun read(): String? = runCatching {
        val encoded = prefs.getString("key", null) ?: return null
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        require(blob.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
        String(cipher.doFinal(blob.copyOfRange(12, blob.size)), StandardCharsets.UTF_8)
    }.getOrNull()

    fun clear() { prefs.edit().remove("key").apply() }
}
