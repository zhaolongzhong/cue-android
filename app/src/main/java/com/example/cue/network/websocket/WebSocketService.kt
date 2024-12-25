package com.example.cue.network.websocket

import android.util.Log
import com.example.cue.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow

@ActivityRetainedScoped
class WebSocketService @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val sharedPreferences: SharedPreferences
) {
    private val tag = "WebSocketService"
    private var webSocket: WebSocket? = null
    private var isReconnecting = false
    private var reconnectAttempts = 0
    private var lastPongReceived = Date()
    private var backgroundJob: Job? = null
    private var pingJob: Job? = null

    private val clientId = BuildConfig.CLIENT_ID
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 5.0
    private val pingInterval = 30L

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val _webSocketMessage = MutableSharedFlow<WebSocketMessage>()
    val webSocketMessage: SharedFlow<WebSocketMessage> = _webSocketMessage.asSharedFlow()

    private val _error = MutableSharedFlow<WebSocketError>()
    val error: SharedFlow<WebSocketError> = _error.asSharedFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            updateConnectionState(ConnectionState.Connected)
            reconnectAttempts = 0
            startPingTimer()
            Timber.d("WebSocket connected")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            updateConnectionState(ConnectionState.Disconnected)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            handleError(WebSocketError.ConnectionFailed(t.message ?: "Connection failed"))
        }
    }

    suspend fun connect() {
        if (_connectionState.value == ConnectionState.Connected) return
        updateConnectionState(ConnectionState.Connecting)

        try {
            establishConnection()
        } catch (e: WebSocketError) {
            handleError(e)
        } catch (e: Exception) {
            handleError(WebSocketError.ConnectionFailed(e.message ?: "Unknown error"))
        }
    }

    private fun establishConnection() {
        val accessToken = sharedPreferences.getString("ACCESS_TOKEN_KEY", null)
            ?: throw WebSocketError.Unauthorized

        val request = Request.Builder()
            .url("${BuildConfig.WS_BASE_URL}/$clientId")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        webSocket = okHttpClient.newWebSocket(request, webSocketListener)
    }

    fun disconnect() {
        backgroundJob?.cancel()
        backgroundJob = null
        pingJob?.cancel()
        pingJob = null
        webSocket?.cancel()
        webSocket = null
        updateConnectionState(ConnectionState.Disconnected)
        reconnectAttempts = 0
        Timber.d("WebSocket disconnected")
    }

    private fun startPingTimer() {
        pingJob?.cancel()
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && _connectionState.value == ConnectionState.Connected) {
                delay(pingInterval * 1000)
                sendPing()
            }
        }
    }

    private fun sendPing() {
        webSocket?.send("")  // OkHttp automatically handles ping/pong frames
        lastPongReceived = Date()
    }

    private fun handleMessage(text: String) {
        try {
            val eventMessage = moshi.adapter(EventMessage::class.java).fromJson(text)
            eventMessage?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    when (it.type) {
                        EventType.CLIENT_CONNECT,
                        EventType.CLIENT_DISCONNECT,
                        EventType.CLIENT_STATUS -> {
                            it.clientStatus?.let { status ->
                                _webSocketMessage.emit(WebSocketMessage.ClientStatus(status))
                            }
                        }
                        EventType.ASSISTANT,
                        EventType.USER -> {
                            if (it.payload is MessagePayload) {
                                _webSocketMessage.emit(WebSocketMessage.MessagePayload(it.payload))
                            }
                        }
                        EventType.GENERIC,
                        EventType.ERROR -> {
                            if (it.payload is GenericPayload) {
                                Timber.d("Received generic message: ${it.payload.message}")
                            }
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Failed to decode message: ${e.message}")
            CoroutineScope(Dispatchers.IO).launch {
                _error.emit(WebSocketError.MessageDecodingFailed)
            }
        }
    }

    private fun handleError(error: WebSocketError) {
        CoroutineScope(Dispatchers.Main).launch {
            updateConnectionState(ConnectionState.Error(error))
            Timber.e("handleError ${error.message}")

            when (error) {
                is WebSocketError.Unauthorized -> {}
                is WebSocketError.ConnectionFailed -> scheduleReconnection()
                is WebSocketError.MessageDecodingFailed -> {}
                is WebSocketError.ReceiveFailed -> scheduleReconnection()
                is WebSocketError.Generic -> {}
                is WebSocketError.Unknown -> {}
            }
        }
    }

    fun send(event: ClientEvent) {
        if (_connectionState.value != ConnectionState.Connected) {
            Timber.e("Attempting to send message while not connected: ${_connectionState.value}")
            handleError(WebSocketError.ConnectionFailed("Socket not connected"))
            return
        }

        try {
            val messageJson = moshi.adapter(ClientEvent::class.java).toJson(event)
            webSocket?.send(messageJson)
        } catch (e: Exception) {
            Timber.e("Failed to serialize message: ${e.message}")
        }
    }

    private fun scheduleReconnection() {
        if (!isReconnecting && reconnectAttempts < maxReconnectAttempts) {
            isReconnecting = true
            val delay = min(baseReconnectDelay * 2.0.pow(reconnectAttempts.toDouble()), 32.0)
            reconnectAttempts++

            Timber.d("Attempting reconnection $reconnectAttempts in $delay seconds")

            CoroutineScope(Dispatchers.Main).launch {
                delay((delay * 1000).toLong())
                try {
                    connect()
                } finally {
                    isReconnecting = false
                }
            }
        } else if (reconnectAttempts >= maxReconnectAttempts) {
            Timber.e("Max reconnection attempts reached. Giving up.")
            updateConnectionState(
                ConnectionState.Error(
                    WebSocketError.ConnectionFailed("Failed to reconnect after $reconnectAttempts attempts")
                )
            )
            isReconnecting = false
        }
    }

    private fun updateConnectionState(newState: ConnectionState) {
        _connectionState.value = newState
        Timber.d("Connection state changed to: $newState")
    }

    companion object {
        private const val TAG = "WebSocketService"
    }
}

sealed class WebSocketMessage {
    data class ClientStatus(val status: ClientStatus) : WebSocketMessage()
    data class MessagePayload(val payload: MessagePayload) : WebSocketMessage()
    data class Event(val message: EventMessage) : WebSocketMessage()
    data class Error(val error: WebSocketError) : WebSocketMessage()
}