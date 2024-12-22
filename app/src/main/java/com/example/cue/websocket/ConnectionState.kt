package com.example.cue.websocket

sealed class ConnectionState {
    object Connected : ConnectionState()
    object Connecting : ConnectionState()
    object Disconnected : ConnectionState()
    data class Error(val error: ConnectionError) : ConnectionState()
}

sealed class ConnectionError {
    object InvalidURL : ConnectionError()
    data class ConnectionFailed(val message: String) : ConnectionError()
    data class ReceiveFailed(val message: String) : ConnectionError()
}
