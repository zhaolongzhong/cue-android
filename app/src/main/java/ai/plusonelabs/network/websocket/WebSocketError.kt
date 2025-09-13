package ai.plusonelabs.network.websocket

sealed class WebSocketError : Exception() {
    data class ConnectionFailed(
        override val message: String = "Failed to establish WebSocket connection",
    ) : WebSocketError()

    data class Unauthorized(
        override val message: String = "Unauthorized WebSocket access",
    ) : WebSocketError()

    data class MessageDecodingFailed(
        override val message: String = "Failed to decode WebSocket message",
    ) : WebSocketError()

    data class ConnectionClosed(
        override val message: String = "WebSocket connection closed",
    ) : WebSocketError()

    data class Generic(
        override val message: String,
    ) : WebSocketError()

    companion object {
        fun from(throwable: Throwable): WebSocketError = when (throwable) {
            is WebSocketError -> throwable
            is java.net.UnknownHostException -> ConnectionFailed("No internet connection")
            is java.net.SocketTimeoutException -> ConnectionFailed("Connection timed out")
            else -> Generic(throwable.message ?: "Unknown WebSocket error occurred")
        }
    }
}
