package com.example.network

/**
 * Network configuration for API requests
 */
data class NetworkConfig(
    val baseUrl: String,
    val apiKey: String,
    val organizationId: String? = null,
    val timeoutSeconds: Long = 30,
    val retryCount: Int = 3,
    val debug: Boolean = false
)