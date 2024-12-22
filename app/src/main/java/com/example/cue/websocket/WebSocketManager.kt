package com.example.cue.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Date
import java.util.UUID
import kotlin.math.pow

private const val TAG = "WebSocketManager"

class WebSocketManager(
    private val baseUrl: String, // check provideWebsocketUrl in /path/to/com.example.cue.di/NetworkModule.kt
    private val clientId: String, // empty is fine
    private val accessToken: String, // check /path/to/com/example/cue/network/NetworkClientImpl.kt for how to add access token
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: WebSocketClient? = null
    private var reconnectJob: Job? = null

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _clientStatuses = MutableStateFlow<List<ClientStatus>>(emptyList())
    val clientStatuses = _clientStatuses.asStateFlow()

    // Reconnection configuration
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val baseReconnectDelay = 5000L // 5 seconds
    private var shouldReconnect = true
    private var isReconnecting = false

    // Ping/Pong configuration
    private val pingInterval = 30000L // 30 seconds
    private val pongTimeout = 40000L // 40 seconds
    private var lastPongReceived = Date()
    private var pingJob: Job? = null
    private var pongCheckJob: Job? = null

    fun connect() {
        if (client != null) {
            Log.w(TAG, "WebSocket manager is already connected")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        shouldReconnect = true
        lastPongReceived = Date()

        val headers = mapOf(
            "Authorization" to "Bearer $accessToken",
            "Content-Type" to "application/json",
        )

        val fullUrl = "$baseUrl/$clientId"

        client = WebSocketClient(fullUrl, headers).also { client ->
            scope.launch {
                client.events.collect { event ->
                    when (event) {
                        is WebSocketEvent.Connected -> {
                            Log.d(TAG, "WebSocket connected")
                            _connectionState.value = ConnectionState.Connected
                            reconnectAttempts = 0
                            lastPongReceived = Date()
                            setupPingPong()
                        }

                        is WebSocketEvent.MessageReceived -> {
                            Log.d(TAG, "Message received: ${event.text}")
                            handleReceivedMessage(event.text)
                        }

                        is WebSocketEvent.Error -> {
                            Log.e(TAG, "WebSocket error", event.error)
                            _connectionState.value = ConnectionState.Error(
                                ConnectionError.ConnectionFailed(
                                    event.error.message ?: "Unknown error",
                                ),
                            )
                            if (shouldReconnect) {
                                scheduleReconnection()
                            }
                        }

                        is WebSocketEvent.Disconnected -> {
                            Log.d(TAG, "WebSocket disconnected")
                            _connectionState.value = ConnectionState.Disconnected
                            if (shouldReconnect) {
                                scheduleReconnection()
                            }
                        }
                    }
                }
            }
            client.connect()
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        cancelPingPong()
        client?.disconnect()
        client = null
        _connectionState.value = ConnectionState.Disconnected
        reconnectAttempts = 0
    }

    fun send(message: String, recipient: String): Boolean {
        if (_connectionState.value != ConnectionState.Connected) {
            Log.e(
                TAG,
                "Attempting to send message while not connected. State: ${_connectionState.value}",
            )
            return false
        }

        val uuid = UUID.randomUUID().toString()
        val messagePayload = JSONObject().apply {
            put("message", message)
            put("recipient", recipient)
            put("websocket_request_id", uuid)
        }

        val eventMessage = JSONObject().apply {
            put("type", "user")
            put("payload", messagePayload)
            put("client_id", clientId)
            put("websocket_request_id", uuid)
        }

        return client?.send(eventMessage) ?: false
    }

    private fun setupPingPong() {
        cancelPingPong()

        // Start ping timer
        pingJob = scope.launch {
            while (isActive) {
                delay(pingInterval)
                if (_connectionState.value == ConnectionState.Connected) {
                    sendProtocolPing()
                }
            }
        }

        // Start pong check timer
        pongCheckJob = scope.launch {
            while (isActive) {
                delay(5000) // Check every 5 seconds
                if (_connectionState.value == ConnectionState.Connected) {
                    val timeSinceLastPong = Date().time - lastPongReceived.time
                    if (timeSinceLastPong > pongTimeout) {
                        Log.e(TAG, "No pong received for ${timeSinceLastPong / 1000} seconds")
                        handleError(ConnectionError.ConnectionFailed("No pong received"))
                    }
                }
            }
        }
    }

    private fun cancelPingPong() {
        pingJob?.cancel()
        pingJob = null
        pongCheckJob?.cancel()
        pongCheckJob = null
    }

    private fun sendProtocolPing() {
        client?.webSocket?.send("") // Empty message acts as a ping
    }

    private fun handleError(error: ConnectionError) {
        _connectionState.value = ConnectionState.Error(error)
        when (error) {
            is ConnectionError.InvalidURL -> {
                Log.e(TAG, "Invalid WebSocket URL")
            }

            is ConnectionError.ConnectionFailed -> {
                Log.e(TAG, "WebSocket connection failed: ${error.message}")
                if (error.message.contains("Socket is not connected") ||
                    error.message.contains("Socket not connected")
                ) {
                    cleanupConnection()
                    scheduleReconnection()
                }
            }

            is ConnectionError.ReceiveFailed -> {
                Log.e(TAG, "WebSocket receive failed: ${error.message}")
                if (error.message.contains("Socket is not connected") ||
                    error.message.contains("Socket not connected")
                ) {
                    cleanupConnection()
                    scheduleReconnection()
                }
            }
        }
    }

    private fun cleanupConnection() {
        cancelPingPong()
        client?.disconnect()
        client = null
    }

    private fun scheduleReconnection() {
        if (isReconnecting) {
            Log.d(TAG, "Reconnection already in progress")
            return
        }

        reconnectJob?.cancel()

        if (!shouldReconnect || reconnectAttempts >= maxReconnectAttempts) {
            Log.e(TAG, "Max reconnection attempts reached or shouldReconnect is false")
            isReconnecting = false
            return
        }

        isReconnecting = true
        val delay = baseReconnectDelay * 2.0.pow(reconnectAttempts.toDouble()).toLong()
        reconnectAttempts++

        Log.d(TAG, "Scheduling reconnection attempt $reconnectAttempts in ${delay / 1000} seconds")

        reconnectJob = scope.launch {
            delay(delay)
            isReconnecting = false
            connect()
        }
    }

    private suspend fun handleReceivedMessage(text: String) {
        try {
            val eventMessage = JSONObject(text)
            val type = eventMessage.getString("type")

            when (type) {
                "ping" -> return // Skip, using protocol level ping/pong
                "pong" -> {
                    Log.d(TAG, "Received unexpected protocol-level pong in application message")
                }

                "client_connect", "client_disconnect", "client_status" -> {
                    val payload = eventMessage.getJSONObject("payload")
                    val clientEventPayload = payload.getJSONObject("client_event")
                    val otherClientId = clientEventPayload.getString("client_id")

                    if (clientId == otherClientId) return

                    val clientStatus = when {
                        clientEventPayload.has("payload") -> {
                            val jsonPayload = clientEventPayload.getJSONObject("payload")
                            ClientStatus(
                                id = otherClientId,
                                assistantId = jsonPayload.optString("assistant_id"),
                                runnerId = jsonPayload.optString("runner_id"),
                                isOnline = true,
                            )
                        }

                        type == "client_disconnect" -> {
                            val existingStatus =
                                _clientStatuses.value.find { it.id == otherClientId }
                            ClientStatus(
                                id = otherClientId,
                                assistantId = existingStatus?.assistantId,
                                runnerId = existingStatus?.runnerId,
                                isOnline = false,
                            )
                        }

                        else -> null
                    }

                    clientStatus?.let { status ->
                        val currentList = _clientStatuses.value.toMutableList()
                        val existingIndex = currentList.indexOfFirst { it.id == status.id }
                        if (existingIndex != -1) {
                            currentList[existingIndex] = status
                        } else {
                            currentList.add(status)
                        }
                        _clientStatuses.value = currentList
                    }
                }

                "assistant", "user" -> {
                    val payload = eventMessage.getJSONObject("payload")
                    val messagePayload = payload.getJSONObject("message")
                    Log.d(
                        TAG,
                        "Received message(id:${messagePayload.optString("msg_id")}): ${
                            messagePayload.optString("message")
                        }",
                    )
                    _messages.emit(text)
                }

                "generic", "error" -> {
                    val payload = eventMessage.getJSONObject("payload")
                    val genericMessage = payload.getJSONObject("generic")
                    Log.d(TAG, "Received generic message: ${genericMessage.optString("message")}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing received message", e)
        }
    }

    companion object {
        private var instance: WebSocketManager? = null

        fun getInstance(baseUrl: String, clientId: String, accessToken: String): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager(baseUrl, clientId, accessToken).also { instance = it }
            }
        }
    }
}

data class ClientStatus(
    val id: String,
    val assistantId: String?,
    val runnerId: String?,
    val isOnline: Boolean,
)
