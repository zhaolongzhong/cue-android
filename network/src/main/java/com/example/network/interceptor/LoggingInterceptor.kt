package com.example.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets
import kotlin.math.min

/**
 * Interceptor that logs request and response details for debugging
 */
class LoggingInterceptor : Interceptor {
    companion object {
        private const val TAG = "NetworkLog"
        private const val MAX_LOG_LENGTH = 4000
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = request.body

        // Log request
        val reqBuffer = Buffer()
        requestBody?.writeTo(reqBuffer)
        val reqString = reqBuffer.readString(StandardCharsets.UTF_8)
        
        log("Request: ${request.method} ${request.url}")
        log("Headers: ${request.headers}")
        if (reqString.isNotEmpty()) {
            log("Body: $reqString")
        }

        // Proceed with request and capture timing
        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val duration = (System.nanoTime() - startTime) / 1e6

        // Log response
        val responseBody = response.body
        val source = responseBody?.source()
        source?.request(Long.MAX_VALUE)
        val respBuffer = source?.buffer
        val respString = respBuffer?.clone()?.readString(StandardCharsets.UTF_8)

        log("Response: ${response.code} (${duration.toInt()}ms)")
        log("Headers: ${response.headers}")
        if (!respString.isNullOrEmpty()) {
            log("Body: $respString")
        }

        return response
    }

    private fun log(message: String) {
        // Split log by max length to avoid Android log truncation
        var i = 0
        while (i < message.length) {
            val end = min(message.length, i + MAX_LOG_LENGTH)
            Log.d(TAG, message.substring(i, end))
            i = end
        }
    }
}