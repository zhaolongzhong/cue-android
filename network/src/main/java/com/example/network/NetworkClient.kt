package com.example.network

import com.example.network.interceptor.AuthInterceptor
import com.example.network.interceptor.LoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Network client for making API requests
 */
class NetworkClient(private val config: NetworkConfig) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
        readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
        writeTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
        addInterceptor(AuthInterceptor(config))
        if (config.debug) {
            addInterceptor(LoggingInterceptor())
        }
    }.build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    /**
     * Creates an implementation of the API endpoints defined by the service interface.
     */
    fun <T> create(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    /**
     * Executes the request and returns a [NetworkResult]
     */
    suspend fun <T> executeRequest(
        request: suspend () -> T
    ): NetworkResult<T> {
        return try {
            NetworkResult.Success(request())
        } catch (e: Exception) {
            NetworkResult.Error(
                message = e.message,
                exception = e
            )
        }
    }
}