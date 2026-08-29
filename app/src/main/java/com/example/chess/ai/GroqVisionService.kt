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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GroqVisionService(context: Context) {
    private val keyStore = SecureGroqKeyStore(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val MODEL = "qwen/qwen3.6-27b"
        private const val BASE = "https://api.groq.com/openai/v1"
    }

    fun savedKey(): String? = keyStore.read()
    fun saveKey(value: String) = keyStore.save(value.trim())
    fun clearKey() = keyStore.clear()

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotBlank()) { "Enter a Groq API key first." }
            val body = JSONObject()
                .put("model", MODEL)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", "Reply with exactly: GROQ_OK")))
                .put("temperature", 0)
                .put("max_completion_tokens", 8)
                .toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE/chat/completions")
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(body).build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(formatApiError(response.code, raw))
                val text = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content").orEmpty().trim()
                if (text.isBlank()) error("Groq responded without a message.")
                "Connection successful • $MODEL"
            }
        }
    }

    suspend fun imageToFen(context: Context, uri: Uri, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey.trim()
            require(key.isNotBlank()) { "Groq API key is missing." }
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Unable to read image.")
            require(bytes.isNotEmpty()) { "Selected image is empty." }
            require(bytes.size <= 20 * 1024 * 1024) { "Image is too large. Choose an image under 20 MB." }
            val mime = resolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val prompt = "Extract the chess position from this image. Ignore arrows, circles, highlights, coordinates, captions, UI and annotations. Identify only the actual pieces on the board and whose turn it is. Return ONLY one complete valid FEN string. No markdown, no explanation, no extra text."
            val content = JSONArray()
                .put(JSONObject().put("type", "text").put("text", prompt))
                .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:$mime;base64,$encoded")))
            val body = JSONObject()
                .put("model", MODEL)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
                .put("temperature", 0)
                .put("max_completion_tokens", 80)
                .put("reasoning_effort", "none")
                .toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE/chat/completions")
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(body).build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(formatApiError(response.code, raw))
                val text = JSONObject(raw).optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content").orEmpty().trim()
                require(text.isNotBlank()) { "Groq returned an empty vision response." }
                validateFen(cleanFen(text))
            }
        }
    }

    private fun cleanFen(raw: String): String = raw.replace("```fen", "", ignoreCase = true)
        .replace("```", "")
        .trim()
        .lineSequence()
        .firstOrNull { it.count { c -> c == '/' } == 7 }
        ?.trim() ?: raw.trim()

    private fun validateFen(fen: String): String {
        val fields = fen.split(Regex("\\s+"))
        require(fields.size >= 2) { "AI did not return a valid FEN." }
        val ranks = fields[0].split('/')
        require(ranks.size == 8) { "AI returned an invalid board layout." }
        ranks.forEach { rank ->
            var count = 0
            for (c in rank) {
                if (c.isDigit()) { require(c in '1'..'8'); count += c.digitToInt() }
                else { require(c in "PNBRQKpnbrqk") { "AI returned an invalid piece." }; count++ }
            }
            require(count == 8) { "AI returned an invalid rank." }
        }
        require(fields[1] == "w" || fields[1] == "b") { "AI returned an invalid side to move." }
        require(fields[0].count { it == 'K' } == 1 && fields[0].count { it == 'k' } == 1) { "FEN must contain both kings." }
        return fen
    }

    private fun formatApiError(code: Int, raw: String): String {
        val message = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        return message?.let { "Groq error (HTTP $code): $it" } ?: "Groq request failed (HTTP $code). Check your API key and internet connection."
    }
}
