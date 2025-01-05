package com.example.cue.network.websocket

import android.content.SharedPreferences
import android.util.Log
import com.example.cue.Environment
import com.squareup.moshi.Moshi
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.min
import kotlin.math.pow

@ActivityRetainedScoped
class WebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    @Named("websocketUrl") private val baseUrl: String,
    private val sharedPreferences: SharedPreferences,
    private val environment: Environment,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val TAG = "WebSocketService"
        private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"
        private const val INITIAL_RETRY_DELAY = 1000L // 1 second
        private const val MAX_RETRY_DELAY = 32000L // 32 seconds
        private const val PING_INTERVAL = 30000L // 30 seconds
    }

    private val scope = CoroutineScope(dispatcher + Job())
    private var webSocket: WebSocket? = null
    private var retryAttempt = 0
    private var pingTimer: Timer? = null
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableStateFlow<EventMessage?>(null)
    val events: StateFlow<EventMessage?> = _events.asStateFlow()

    private val eventMessageAdapter = moshi.adapter(EventMessage::class.java)

    fun connect() {
        Log.d(TAG, "Attempting to connect to WebSocket")
        if (_connectionState.value is ConnectionState.Connecting || _connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Already connecting or connected")
            return
        }

        _connectionState.value = ConnectionState.Connecting

        val token = sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
        val clientId = environment.clientId

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No access token found")
            _connectionState.value = ConnectionState.Error(WebSocketError.Unauthorized())
            return
        }

        val wsUrl = "$baseUrl/$clientId"
        Log.d(TAG, "Connecting to WebSocket URL: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        stopPingTimer()
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
        retryAttempt = 0
    }

    fun send(message: EventMessage) {
        val json = eventMessageAdapter.toJson(message)
        webSocket?.send(json)
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                _connectionState.value = ConnectionState.Connected
                retryAttempt = 0
                startPingTimer()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            scope.launch {
                try {
                    val message = eventMessageAdapter.fromJson(text)
                    when (message?.type) {
                        EventMessageType.CLIENT_STATUS -> {
                            _events.value = message
                        }

                        EventMessageType.CLIENT_CONNECT -> {
                            _events.value = message
                        }

                        EventMessageType.CLIENT_DISCONNECT -> {
                            _events.value = message
                        }

                        EventMessageType.PONG -> {
                            // Handle pong internally
                        }

                        else -> {
                            _events.value = message
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode message: ${e.message}", e)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            scope.launch {
                val error = WebSocketError.from(t)
                _connectionState.value = ConnectionState.Error(error)
                scheduleReconnect()
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            scope.launch {
                stopPingTimer()
                _connectionState.value = ConnectionState.Error(
                    WebSocketError.ConnectionClosed("Connection closed: $reason"),
                )
                if (code != 1000) { // Not a normal closure
                    scheduleReconnect()
                }
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delay = calculateRetryDelay()
            TimeUnit.MILLISECONDS.sleep(delay)
            retryAttempt++
            connect()
        }
    }

    private fun calculateRetryDelay(): Long {
        return min(
            INITIAL_RETRY_DELAY * 2.0.pow(retryAttempt.toDouble()).toLong(),
            MAX_RETRY_DELAY,
        )
    }

    private fun startPingTimer() {
        stopPingTimer()
        pingTimer = Timer().apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        val pingMessage = EventMessage(
                            type = EventMessageType.PING,
                            payload = MessagePayload(
                                message = "ping",
                                sender = "client",
                                recipient = null,
                                websocketRequestId = UUID.randomUUID().toString(),
                                metadata = null,
                                userId = "",
                                msgId = null,
                                payload = null,
                            ),
                            clientId = null,
                            metadata = null,
                            websocketRequestId = null,
                        )
                        send(pingMessage)
                    }
                },
                PING_INTERVAL, PING_INTERVAL,
            )
        }
    }

    private fun stopPingTimer() {
        pingTimer?.cancel()
        pingTimer = null
    }
}
