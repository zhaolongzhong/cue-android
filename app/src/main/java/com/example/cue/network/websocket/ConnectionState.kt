package com.example.cue.network.websocket

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val error: WebSocketError) : ConnectionState()

    override fun toString(): String = when (this) {
        is Connected -> "Connected"
        is Connecting -> "Connecting"
        is Disconnected -> "Disconnected"
        is Error -> "Error: $error"
    }
}

sealed class WebSocketError : Exception() {
    data class ConnectionFailed(override val message: String) : WebSocketError()
    data class ReceiveFailed(override val message: String) : WebSocketError()
    data object MessageDecodingFailed : WebSocketError()
    data object Unauthorized : WebSocketError()
    data class Generic(override val message: String?) : WebSocketError()
    data class Unknown(override val message: String?) : WebSocketError()

    override val message: String
        get() = when (this) {
            is ConnectionFailed -> "Connection Failed: $message"
            is MessageDecodingFailed -> "Failed to decode message"
            is ReceiveFailed -> "Receive Failed: $message"
            is Unauthorized -> "Unauthorized access"
            is Generic -> message ?: "An error occurred"
            is Unknown -> message ?: "An unknown error occurred"
        }
}