package com.example.cue.network

interface NetworkClient {
    suspend fun <T> postFormUrlEncoded(
        endpoint: String,
        body: Map<String, String>,
        responseType: Class<T>,
    ): T

    suspend fun <T> get(
        endpoint: String,
        responseType: Class<T>,
    ): T

    suspend fun <T> post(
        endpoint: String,
        body: Map<String, Any?>,
        responseType: Class<T>,
    ): T

    suspend fun <T> put(
        endpoint: String,
        body: Map<String, Any?>,
        responseType: Class<T>,
    ): T

    suspend fun <T> delete(
        endpoint: String,
        responseType: Class<T>,
    ): T
}

sealed class NetworkError : Exception() {
    data class Unauthorized(
        override val message: String = "Unauthorized access",
    ) : NetworkError()

    data class HttpError(
        val code: Int,
        override val message: String,
    ) : NetworkError()

    data class NetworkFailure(
        override val message: String,
    ) : NetworkError()

    data class ParseError(
        override val message: String,
    ) : NetworkError()

    companion object {
        fun from(throwable: Throwable): NetworkError = when (throwable) {
            is NetworkError -> throwable
            is java.net.UnknownHostException -> NetworkFailure("No internet connection")
            is java.net.SocketTimeoutException -> NetworkFailure("Request timed out")
            else -> NetworkFailure(throwable.message ?: "Unknown error occurred")
        }
    }
}
