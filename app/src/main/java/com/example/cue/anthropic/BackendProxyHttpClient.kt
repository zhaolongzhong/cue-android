package com.example.cue.anthropic

import android.content.SharedPreferences
import android.util.Log
import com.anthropic.core.RequestOptions
import com.anthropic.core.http.Headers
import com.anthropic.core.http.HttpClient
import com.anthropic.core.http.HttpRequest
import com.anthropic.core.http.HttpResponse
import com.example.cue.settings.apikeys.ApiKeyType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Custom HttpClient that proxies Anthropic SDK requests to our backend server
 * Maps /v1/messages -> /api/v1/chat/completions while preserving request/response format
 */
class BackendProxyHttpClient(
    private val baseUrl: String,
    private val authToken: String,
    private val sharedPreferences: SharedPreferences? = null,
) : HttpClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            try {
                chain.proceed(chain.request())
            } catch (e: Exception) {
                throw RuntimeException("Network request failed: ${e.localizedMessage}", e)
            }
        }
        .build()

    companion object {
        const val TAG = "BackendProxyHttpClient"
    }

    override fun execute(
        request: HttpRequest,
        requestOptions: RequestOptions,
    ): HttpResponse {
        val fullUrl = constructFullUrl(request)
        Log.d(TAG, "Intercepting SDK request: ${request.method} $fullUrl")

        val transformedRequest = transformRequest(request)
        val okHttpRequest = buildOkHttpRequest(transformedRequest)
        Log.d(TAG, "Executing backend request: ${okHttpRequest.method} ${okHttpRequest.url}")

        okHttpClient.newCall(okHttpRequest).execute().use { okHttpResponse ->
            Log.d(TAG, "Backend response code: ${okHttpResponse.code}")

            val responseBody = okHttpResponse.body?.string() ?: ""
            Log.d(TAG, "Backend response body: $responseBody")
            return BackendHttpResponse(
                statusCode = okHttpResponse.code,
                headers = convertOkHttpHeadersToSdkHeaders(okHttpResponse.headers),
                body = ByteArrayInputStream(responseBody.toByteArray()),
            )
        }
    }

    override fun executeAsync(
        request: HttpRequest,
        requestOptions: RequestOptions,
    ): CompletableFuture<HttpResponse> = CompletableFuture.supplyAsync {
        execute(request, requestOptions)
    }

    override fun close() {
        // Close any resources if needed
        // OkHttp client resources will be handled by the client itself
    }

    private fun convertHeadersToMap(headers: Headers): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        headers.names().forEach { name ->
            map[name] = headers.values(name)
        }
        return map
    }

    private fun convertOkHttpHeadersToSdkHeaders(okHttpHeaders: okhttp3.Headers): Headers {
        val builder = Headers.builder()
        okHttpHeaders.names().forEach { name ->
            okHttpHeaders.values(name).forEach { value ->
                builder.put(name, value)
            }
        }
        return builder.build()
    }

    private fun constructFullUrl(request: HttpRequest): String {
        val baseUrl = request.baseUrl ?: ""
        val path = if (request.pathSegments.isEmpty()) "" else "/" + request.pathSegments.joinToString("/")
        val queryString = if (request.queryParams.isEmpty()) {
            ""
        } else {
            "?" +
                request.queryParams.keys().joinToString("&") { key ->
                    request.queryParams.values(key).joinToString("&") { value -> "$key=$value" }
                }
        }
        return "$baseUrl$path$queryString"
    }

    private fun transformRequest(request: HttpRequest): TransformedRequest {
        val originalUrl = constructFullUrl(request)
        Log.d(TAG, "Original URL: $originalUrl")
        val newUrl = when {
            originalUrl.contains("/v1/messages") -> {
                val newEndpoint = "$baseUrl/api/v1/chat/completions"
                Log.d(TAG, "Mapping /v1/messages -> $newEndpoint")
                newEndpoint
            }
            else -> {
                Log.w(TAG, "Unmapped endpoint: $originalUrl")
                originalUrl
            }
        }

        val body = request.body?.let { requestBody ->
            val outputStream = java.io.ByteArrayOutputStream()
            requestBody.writeTo(outputStream)
            outputStream.toByteArray()
        }

        return TransformedRequest(
            method = request.method.toString(),
            url = newUrl,
            headers = convertHeadersToMap(request.headers),
            body = body,
        )
    }

    private fun buildOkHttpRequest(transformedRequest: TransformedRequest): Request {
        val builder = Request.Builder()
            .url(transformedRequest.url)

        transformedRequest.headers.forEach { (name, values) ->
            values.forEach { value ->
                builder.addHeader(name, value)
            }
        }

        builder.header("Authorization", "Bearer $authToken")
        val userApiKey = sharedPreferences?.getString(ApiKeyType.ANTHROPIC.key, "")
        if (!userApiKey.isNullOrEmpty()) {
            builder.header("X-Anthropic-API-Key", userApiKey)
            Log.d(TAG, "Added user's Anthropic API key")
        } else {
            Log.w(TAG, "No Anthropic API key found in user settings")
        }

        when (transformedRequest.method.uppercase()) {
            "POST", "PUT", "PATCH" -> {
                val requestBody = transformedRequest.body?.let {
                    String(it).toRequestBody("application/json".toMediaType())
                } ?: "".toRequestBody("application/json".toMediaType())

                builder.method(transformedRequest.method, requestBody)
            }
            else -> {
                builder.method(transformedRequest.method, null)
            }
        }

        return builder.build()
    }

    private data class TransformedRequest(
        val method: String,
        val url: String,
        val headers: Map<String, List<String>>,
        val body: ByteArray?,
    )

    private class BackendHttpResponse(
        private val statusCode: Int,
        private val headers: Headers,
        private val body: InputStream,
    ) : HttpResponse {

        override fun statusCode(): Int = statusCode

        override fun headers(): Headers = headers

        override fun body(): InputStream = body

        override fun close() {
            try {
                body.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing response body", e)
            }
        }
    }
}
