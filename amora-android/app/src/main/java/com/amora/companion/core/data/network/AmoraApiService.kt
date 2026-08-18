package com.amora.companion.core.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class HealthResponse(
    val status: String,
    val groq_api_valid: Boolean,
    val database_connected: Boolean,
    val environment: String,
    val backend_version: String
)

data class WeatherHourlyItem(
    val time: String,
    val temp: String,
    val icon: String
)

data class WeatherDailyItem(
    val day: String,
    val tempRange: String,
    val condition: String,
    val icon: String
)

data class WeatherResponse(
    val temperature_celsius: Float,
    val condition: String,
    val city: String,
    val humidity: Int,
    val wind_speed_kmh: Float,
    val uv_index: Float = 0f,
    val display_text: String,
    val hourly: List<WeatherHourlyItem> = emptyList(),
    val weekly: List<WeatherDailyItem> = emptyList()
)

data class NewsArticle(
    val title: String,
    val description: String = "",
    val content: String = "",
    val source: String = "GNews",
    val url: String = "#",
    val published_at: String = "",
    val category: String = "general"
)

data class NewsResponse(
    val status: String,
    val articles: List<NewsArticle>
)

data class CallSummarizeRequest(
    val caller: String,
    val reason: String,
    @SerializedName("is_emergency") val isEmergency: Boolean = false
)

data class ChatRequest(val message: String)
data class ChatResponse(
    val reply: String,
    val intent: Any? = null,
    /** "groq" | "trained" | "fallback" — lets UI know the answer source */
    val source: String = "unknown"
)

// ── Training data models ───────────────────────────────────────────────────────

data class OfflineAnswerResponse(val found: Boolean, val answer: String)
data class TrainRequest(val question: String, val answer: String)
data class TrainResponse(val saved: Boolean, val message: String)

data class TrainingPair(
    val id: Int,
    val question: String,
    val answer: String,
    val use_count: Int,
    val created_at: Double
)
data class TrainingDataResponse(val count: Int, val pairs: List<TrainingPair>)

interface AmoraApiService {
    @GET("v1/health")
    suspend fun checkHealth(): Response<HealthResponse>

    @GET("v1/weather")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("city") city: String
    ): Response<WeatherResponse>

    @GET("v1/news")
    suspend fun getNews(): Response<NewsResponse>

    @POST("v1/chat")
    suspend fun sendChat(@Body request: ChatRequest): Response<ChatResponse>

    /** Sends a screened call's caller/reason to the backend for a 1-line AI summary + action items. */
    @POST("v1/calls/summarize")
    suspend fun summarizeCall(@Body request: CallSummarizeRequest): Response<Map<String, Any>>

    // ── Training endpoints ─────────────────────────────────────────────────────

    /** Search trained Q&A pairs for a matching offline answer */
    @GET("v1/chat/trained")
    suspend fun searchOffline(@Query("q") question: String): Response<OfflineAnswerResponse>

    /** Manually add a Q&A training pair (teach Amora something) */
    @POST("v1/chat/train")
    suspend fun trainPair(@Body request: TrainRequest): Response<TrainResponse>

    /** List all saved training pairs (for Settings screen) */
    @GET("v1/chat/training-data")
    suspend fun getTrainingData(): Response<TrainingDataResponse>

    /** Delete a specific training pair by ID */
    @DELETE("v1/chat/training-data/{id}")
    suspend fun deleteTrainingPair(@Path("id") id: Int): Response<Map<String, Any>>

    @DELETE("v1/memory/wipe")
    suspend fun wipeAllData(): Response<Map<String, String>>
}

