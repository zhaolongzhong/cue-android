package com.example.cue.network

import com.example.cue.auth.AuthService
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val baseUrl: String
) : NetworkClient {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun <T> get(path: String): T {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .build()
        
        return executeRequest(request)
    }

    override suspend fun <T> post(path: String, body: Map<String, Any?>): T {
        val jsonBody = moshi.adapter(Map::class.java).toJson(body)
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request)
    }

    override suspend fun <T> put(path: String, body: Map<String, Any?>): T {
        val jsonBody = moshi.adapter(Map::class.java).toJson(body)
        val request = Request.Builder()
            .url("$baseUrl$path")
            .put(jsonBody.toRequestBody(jsonMediaType))
            .build()
        
        return executeRequest(request)
    }

    override suspend fun <T> delete(path: String): T {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .delete()
            .build()
        
        return executeRequest(request)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> executeRequest(request: Request): T {
        val response = try {
            okHttpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw NetworkError.NetworkFailure(e.message ?: "Network request failed")
        }

        when (response.code) {
            401 -> throw NetworkError.Unauthorized
            in 400..599 -> throw NetworkError.HttpError(
                response.code,
                response.body?.string() ?: "HTTP ${response.code}"
            )
        }

        val body = response.body?.string()
            ?: throw NetworkError.UnexpectedResponse("Empty response body")

        val type = getResponseType<T>()
        return try {
            moshi.adapter<T>(type.java).fromJson(body) as T
        } catch (e: Exception) {
            throw NetworkError.UnexpectedResponse("Failed to parse response: ${e.message}")
        }
    }

    private inline fun <reified T> getResponseType() = T::class
}