package com.amora.companion.core.di

import android.util.Log
import com.amora.companion.core.data.network.AmoraApiService
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        userPreferencesRepository: UserPreferencesRepository
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("AmoraNetwork", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Cache the backend URL in a MutableStateFlow to avoid runBlocking on the interceptor thread.
        // The flow is observed on a dedicated coroutine scope and the cached value is read synchronously
        // in the interceptor — this is safe because it's non-blocking (reads a StateFlow value).
        val cachedBackendUrl = MutableStateFlow("http://10.0.2.2:8000/")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        userPreferencesRepository.backendUrl.onEach { url ->
            val clean = if (url.endsWith("/")) url else "$url/"
            cachedBackendUrl.value = clean
        }.launchIn(scope)

        // Dynamic URL + ngrok bypass Interceptor (non-blocking — reads cached StateFlow value)
        val dynamicHostInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            val cleanBaseUrl = cachedBackendUrl.value
            val path = originalRequest.url.encodedPath.removePrefix("/")
            val targetUrlStr = cleanBaseUrl + path +
                    (if (originalRequest.url.query != null) "?${originalRequest.url.query}" else "")
            val targetHttpUrl = targetUrlStr.toHttpUrlOrNull()

            val requestBuilder = originalRequest.newBuilder()
                .header("ngrok-skip-browser-warning", "true")
                .header("User-Agent", "AMORA-Companion-Android")

            if (targetHttpUrl != null) {
                requestBuilder.url(targetHttpUrl)
            }

            val request = requestBuilder.build()
            Log.d("AmoraNetwork", "Executing request to: ${request.url}")
            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicHostInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAmoraApiService(
        okHttpClient: OkHttpClient
    ): AmoraApiService {
        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1:8000/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AmoraApiService::class.java)
    }
}
