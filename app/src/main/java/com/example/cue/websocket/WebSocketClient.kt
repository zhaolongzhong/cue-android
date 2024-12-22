package com.example.cue.websocket

import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

private const val TAG = "WebSocketClient"

sealed class WebSocketEvent {
    data class Connected(val webSocket: WebSocket) : WebSocketEvent()
    data class MessageReceived(val text: String) : WebSocketEvent()
    data class Error(val error: Throwable) : WebSocketEvent()
    object Disconnected : WebSocketEvent()
}

class WebSocketClient(
    private val url: String,
    private val headers: Map<String, String> = emptyMap(),
) {
    private val client = OkHttpClient()
    var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 0)
    private val connectionStatus = Channel<Boolean>(Channel.BUFFERED)

    val events = _events.asSharedFlow()

    fun connect() {
        if (webSocket != null) {
            Log.w(TAG, "WebSocket is already connected")
            return
        }

        val request = Request.Builder()
            .url(url)
            .apply {
                headers.forEach { (key, value) ->
                    addHeader(key, value)
                }
            }
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
        Log.d(TAG, "Connecting to WebSocket at $url")
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnected by user")
        webSocket = null
        Log.d(TAG, "Disconnected from WebSocket")
    }

    fun send(message: String): Boolean {
        Log.d(TAG, "Sending message: $message")
        return webSocket?.send(message) ?: false
    }

    fun send(json: JSONObject): Boolean {
        return send(json.toString())
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connection opened")
            connectionStatus.trySend(true)
            _events.tryEmit(WebSocketEvent.Connected(webSocket))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received text message: $text")
            _events.tryEmit(WebSocketEvent.MessageReceived(text))
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Received bytes message: ${bytes.utf8()}")
            _events.tryEmit(WebSocketEvent.MessageReceived(bytes.utf8()))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code - $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code - $reason")
            connectionStatus.trySend(false)
            _events.tryEmit(WebSocketEvent.Disconnected)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            connectionStatus.trySend(false)
            _events.tryEmit(WebSocketEvent.Error(t))
        }
    }
}
