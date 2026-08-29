package com.example.chess.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {
    private const val PREFS_NAME = "secure_chess_prefs"
    private const val KEY_GROQ_API_KEY = "groq_api_key"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getGroqApiKey(context: Context): String? {
        return getPrefs(context).getString(KEY_GROQ_API_KEY, null)
    }

    fun setGroqApiKey(context: Context, key: String) {
        getPrefs(context).edit().putString(KEY_GROQ_API_KEY, key).apply()
    }
}
