package com.example.cue.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val TAG = "WebSocketManager"

class WebSocketManager(
    private val baseUrl: String,
    private val headers: Map<String, String> = emptyMap(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: WebSocketClient? = null
    
    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun connect() {
        if (client != null) {
            Log.w(TAG, "WebSocket manager is already connected")
            return
        }

        client = WebSocketClient(baseUrl, headers).also { client ->
            scope.launch {
                client.events.collect { event ->
                    when (event) {
                        is WebSocketEvent.Connected -> {
                            Log.d(TAG, "WebSocket connected")
                        }
                        is WebSocketEvent.MessageReceived -> {
                            Log.d(TAG, "Message received: ${event.text}")
                            _messages.emit(event.text)
                        }
                        is WebSocketEvent.Error -> {
                            Log.e(TAG, "WebSocket error", event.error)
                            // Implement reconnection logic here
                            reconnect()
                        }
                        is WebSocketEvent.Disconnected -> {
                            Log.d(TAG, "WebSocket disconnected")
                            // Implement reconnection logic here if needed
                            reconnect()
                        }
                    }
                }
            }
            client.connect()
        }
    }

    fun disconnect() {
        client?.disconnect()
        client = null
    }

    fun send(message: String): Boolean {
        return client?.send(message) ?: false
    }

    fun send(json: JSONObject): Boolean {
        return send(json.toString())
    }

    private fun reconnect() {
        Log.d(TAG, "Attempting to reconnect...")
        disconnect()
        connect()
    }

    companion object {
        private var instance: WebSocketManager? = null

        fun getInstance(baseUrl: String, headers: Map<String, String> = emptyMap()): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager(baseUrl, headers).also { instance = it }
            }
        }
    }
}