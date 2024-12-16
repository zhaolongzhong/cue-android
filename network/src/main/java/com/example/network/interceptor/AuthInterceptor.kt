package com.example.network.interceptor

import com.example.network.NetworkConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds authentication headers to requests
 */
class AuthInterceptor(private val config: NetworkConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            addHeader("Authorization", "Bearer ${config.apiKey}")
            config.organizationId?.let {
                addHeader("OpenAI-Organization", it)
            }
            addHeader("Content-Type", "application/json")
        }.build()

        return chain.proceed(request)
    }
}