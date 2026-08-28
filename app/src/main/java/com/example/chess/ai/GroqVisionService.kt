package com.example.chess.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqVisionService(context: Context) {
    private val keyStore = SecureGroqKeyStore(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"
        private const val BASE = "https://api.groq.com/openai/v1"
    }

    fun savedKey(): String? = keyStore.read()
    fun saveKey(value: String) = keyStore.save(value.trim())
    fun clearKey() = keyStore.clear()

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        requestModels(apiKey.trim())
    }

    private fun requestModels(apiKey: String): Result<String> = runCatching {
        require(apiKey.isNotBlank()) { "Groq API key is missing." }
        val request = Request.Builder().url("$BASE/models")
            .header("Authorization", "Bearer $apiKey")
            .get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Groq rejected the key (HTTP ${response.code}).")
            "Connection successful"
        }
    }

    suspend fun imageToFen(context: Context, uri: Uri, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Groq API key is missing." }
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Unable to read image.")
            require(bytes.isNotEmpty()) { "Selected image is empty." }
            require(bytes.size <= 20 * 1024 * 1024) { "Image is too large. Choose an image under 20 MB." }
            val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val prompt = "Extract the chess position from this image. Ignore arrows, circles, highlights, coordinates, captions, UI and annotations. Identify only the actual pieces on the board and whose turn it is. Return ONLY one valid complete FEN string, with no markdown, no explanation, and no extra text."
            val content = org.json.JSONArray()
                .put(JSONObject().put("type", "text").put("text", prompt))
                .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:$mime;base64,$encoded")))
            val message = JSONObject().put("role", "user").put("content", content)
            val body = JSONObject()
                .put("model", MODEL)
                .put("temperature", 0)
                .put("max_tokens", 80)
                .put("messages", org.json.JSONArray().put(message))
                .toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body).build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) error("Vision request failed (HTTP ${response.code}).")
                val text = JSONObject(raw).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").optString("content").trim()
                validateFen(cleanFen(text))
            }
        }
    }

    private fun cleanFen(raw: String): String {
        return raw.replace("```fen", "", ignoreCase = true)
            .replace("```", "")
            .trim().lineSequence().firstOrNull { it.count { c -> c == '/' } == 7 }?.trim() ?: raw.trim()
    }

    private fun validateFen(fen: String): String {
        val fields = fen.split(Regex("\\s+"))
        require(fields.size >= 2) { "AI did not return a valid FEN." }
        val ranks = fields[0].split('/')
        require(ranks.size == 8) { "AI returned an invalid board layout." }
        ranks.forEach { rank ->
            var count = 0
            for (c in rank) {
                if (c.isDigit()) count += c.digitToInt()
                else { require(c in "PNBRQKpnbrqk") { "AI returned an invalid piece." }; count++ }
            }
            require(count == 8) { "AI returned an invalid rank." }
        }
        require(fields[1] == "w" || fields[1] == "b") { "AI returned an invalid side to move." }
        require(fields[0].count { it == 'K' } == 1 && fields[0].count { it == 'k' } == 1) { "FEN must contain both kings." }
        return fen
    }
}
