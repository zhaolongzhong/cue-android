package com.example.cue.network

import android.content.SharedPreferences
import com.example.cue.utils.AppLog
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets.UTF_8

@Singleton
class NetworkClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val baseUrl: String,
    private val sharedPreferences: SharedPreferences,
) : NetworkClient {

    companion object {
        private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"
    }
    private fun getAuthorizationHeader(): String? {
        val token = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        return if (!token.isNullOrEmpty()) "Bearer $token" else null
    }

    private fun Request.Builder.addAuthHeaderIfNeeded(): Request.Builder {
        getAuthorizationHeader()?.let { authHeader ->
            addHeader("Authorization", authHeader)
        }
        return this
    }

    override suspend fun <T> postFormUrlEncoded(
        endpoint: String,
        body: Map<String, String>,
        responseType: Class<T>,
    ): T = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder().apply {
            body.forEach { (key, value) ->
                addEncoded(
                    URLEncoder.encode(key, UTF_8.toString()),
                    URLEncoder.encode(value, UTF_8.toString()),
                )
            }
        }.build()

        executeRequest(
            request = Request.Builder()
                .url("$baseUrl$endpoint")
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build(),
            clazz = responseType,
        )
    }

    override suspend fun <T> get(
        endpoint: String,
        responseType: Class<T>,
    ): T = withContext(Dispatchers.IO) {
        executeRequest(
            request = Request.Builder()
                .url("$baseUrl$endpoint")
                .get()
                .addAuthHeaderIfNeeded()
                .build(),
            clazz = responseType,
        )
    }

    override suspend fun <T> post(
        endpoint: String,
        body: Map<String, Any?>,
        responseType: Class<T>,
    ): T = withContext(Dispatchers.IO) {
        val jsonBody = body.filterValues { it != null }
        val jsonString = moshi.adapter(Map::class.java).toJson(jsonBody)

        executeRequest(
            request = Request.Builder()
                .url("$baseUrl$endpoint")
                .post(JsonBody(jsonString))
                .addHeader("Content-Type", "application/json")
                .addAuthHeaderIfNeeded()
                .build(),
            clazz = responseType,
        )
    }

    override suspend fun <T> put(
        endpoint: String,
        body: Map<String, Any?>,
        responseType: Class<T>,
    ): T = withContext(Dispatchers.IO) {
        val jsonBody = body.filterValues { it != null }
        val jsonString = moshi.adapter(Map::class.java).toJson(jsonBody)

        executeRequest(
            request = Request.Builder()
                .url("$baseUrl$endpoint")
                .put(JsonBody(jsonString))
                .addHeader("Content-Type", "application/json")
                .addAuthHeaderIfNeeded()
                .build(),
            clazz = responseType,
        )
    }

    override suspend fun <T> delete(
        endpoint: String,
        responseType: Class<T>,
    ): T = withContext(Dispatchers.IO) {
        executeRequest(
            request = Request.Builder()
                .url("$baseUrl$endpoint")
                .delete()
                .addAuthHeaderIfNeeded()
                .build(),
            clazz = responseType,
        )
    }

    private suspend fun <T> executeRequest(request: Request, clazz: Class<T>): T {
        AppLog.info("NetworkClient: ${request.method} ${request.url}")
        try {
            okHttpClient.newCall(request).execute().use { response ->
                AppLog.info("NetworkClient Response: ${response.code} for ${request.url}")
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    AppLog.error("NetworkClient Error: ${response.code} - $errorBody")
                    when (response.code) {
                        401 -> throw NetworkError.Unauthorized()
                        else -> throw NetworkError.HttpError(response.code, response.message)
                    }
                }
                val responseBody = response.body?.string()
                    ?: throw NetworkError.ParseError("Empty response body")

                if (clazz == String::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return responseBody as T
                }

                return try {
                    moshi.adapter(clazz).fromJson(responseBody)
                        ?: throw NetworkError.ParseError("Failed to parse response")
                } catch (e: Exception) {
                    throw NetworkError.ParseError("Failed to parse response: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw when (e) {
                is NetworkError -> e
                is IOException -> NetworkError.NetworkFailure("Network connection failed: ${e.message}")
                else -> NetworkError.NetworkFailure(e.message ?: "Network request failed")
            }
        }
    }

    private class JsonBody(private val json: String) : RequestBody() {
        override fun contentType() = "application/json; charset=utf-8".toMediaType()

        override fun writeTo(sink: BufferedSink) {
            sink.writeUtf8(json)
        }

        override fun contentLength(): Long = json.toByteArray(Charsets.UTF_8).size.toLong()
    }
}
