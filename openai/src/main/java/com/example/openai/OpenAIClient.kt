package com.example.openai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException

internal class OpenAIClient(
    private val configuration: OpenAI.Configuration,
    private val json: Json = Json { ignoreUnknownKeys = true },
    httpClient: OkHttpClient? = null
) {
    private val client = httpClient ?: OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    suspend inline fun <reified T> send(
        endpoint: String,
        method: String,
        body: Any? = null
    ): T = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${configuration.baseUrl}/$endpoint")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${configuration.apiKey}")
            .apply {
                when (method) {
                    "GET" -> get()
                    "POST" -> {
                        val jsonBody = body?.let { json.encodeToString(it) }
                        post(jsonBody?.toRequestBody("application/json".toMediaType())
                            ?: "".toRequestBody())
                    }
                    else -> throw IllegalArgumentException("Unsupported method: $method")
                }
            }
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: throw OpenAI.Error.InvalidResponse

            if (!response.isSuccessful) {
                throw OpenAI.Error.ApiError(responseBody)
            }

            try {
                json.decodeFromString<T>(responseBody)
            } catch (e: Exception) {
                throw OpenAI.Error.DecodingError(e)
            }
        } catch (e: IOException) {
            throw OpenAI.Error.NetworkError(e)
        }
    }
}