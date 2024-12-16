package com.example.network

/**
 * A sealed class that represents the result of a network operation
 */
sealed class NetworkResult<out T> {
    /**
     * Represents a successful network operation with data
     */
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * Represents a failed network operation with error details
     */
    data class Error(
        val code: Int? = null,
        val message: String? = null,
        val exception: Throwable? = null
    ) : NetworkResult<Nothing>()

    /**
     * Utility function to handle both success and error cases
     */
    suspend fun onSuccess(action: suspend (T) -> Unit): NetworkResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Utility function to handle error cases
     */
    suspend fun onError(action: suspend (Error) -> Unit): NetworkResult<T> {
        if (this is Error) {
            action(this)
        }
        return this
    }

    /**
     * Maps the success data to a new type
     */
    suspend fun <R> map(transform: suspend (T) -> R): NetworkResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
}