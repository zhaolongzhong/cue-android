package com.example.cue.anthropic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

class AnthropicClient(
    private val configuration: Configuration,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val moshi = Moshi.Builder()
        .add(ContentBlockAdapter.FACTORY)
        .add(KotlinJsonAdapterFactory())
        .build()

    sealed class Error : Exception() {
        data class NetworkError(val cause: Throwable) : Error()
        data class ApiError(val message: String) : Error()
        data class DecodingError(val cause: Throwable) : Error()
        object InvalidResponse : Error()
    }

    data class Configuration(
        val baseUrl: String = "https://api.anthropic.com/v1",
        val apiKey: String
    )

    suspend fun <T> send(
        endpoint: String,
        method: String,
        body: Any? = null,
        responseType: Class<T>
    ): T = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("${configuration.baseUrl}/$endpoint")
                .header("Content-Type", "application/json")
                .header("x-api-key", configuration.apiKey)
                .header("anthropic-version", "2023-06-01")

            if (body != null) {
                val jsonAdapter = moshi.adapter(body::class.java)
                val jsonBody = jsonAdapter.toJson(body)
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                requestBuilder.method(method, requestBody)
            } else {
                requestBuilder.method(method, null)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: throw Error.InvalidResponse

            if (!response.isSuccessful) {
                throw Error.ApiError(responseBody)
            }

            val jsonAdapter = moshi.adapter(responseType)
            jsonAdapter.fromJson(responseBody) ?: throw Error.InvalidResponse

        } catch (e: IOException) {
            throw Error.NetworkError(e)
        } catch (e: Error) {
            throw e
        } catch (e: Exception) {
            throw Error.DecodingError(e)
        }
    }
}