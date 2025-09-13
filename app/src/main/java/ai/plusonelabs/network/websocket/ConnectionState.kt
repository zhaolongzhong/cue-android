package ai.plusonelabs.network.websocket

sealed class ConnectionState {
    data object Disconnected : ConnectionState() {
        override val description = "Disconnected"
    }

    data object Connecting : ConnectionState() {
        override val description = "Connecting"
    }

    data object Connected : ConnectionState() {
        override val description = "Connected"
    }

    data class Error(val error: WebSocketError) : ConnectionState() {
        override val description = "Error: ${error.message}"
    }

    abstract val description: String
}
