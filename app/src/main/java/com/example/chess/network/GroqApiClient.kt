package com.example.chess.network

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    val model: String = "llama-3.2-90b-vision-preview",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class GroqMessage(
    val role: String,
    val content: List<GroqContent>
)

@JsonClass(generateAdapter = true)
data class GroqContent(
    val type: String,
    val text: String? = null,
    val image_url: GroqImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class GroqImageUrl(
    val url: String // data:image/jpeg;base64,...
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    val choices: List<GroqChoice>
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val message: GroqMessageResponse
)

@JsonClass(generateAdapter = true)
data class GroqMessageResponse(
    val role: String,
    val content: String
)

interface GroqApiService {
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse
}

object GroqApiClient {
    private const val BASE_URL = "https://api.groq.com/openai/v1/"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val apiService: GroqApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }
}
