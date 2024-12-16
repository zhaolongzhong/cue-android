package com.example.cue.network

interface NetworkClient {
    suspend fun <T> get(path: String): T
    suspend fun <T> post(path: String, body: Map<String, Any?>): T
    suspend fun <T> put(path: String, body: Map<String, Any?>): T
    suspend fun <T> delete(path: String): T
}

sealed class NetworkError : Exception() {
    object Unauthorized : NetworkError()
    data class HttpError(val code: Int, override val message: String) : NetworkError()
    data class NetworkFailure(override val message: String) : NetworkError()
    data class UnexpectedResponse(override val message: String) : NetworkError()
}