package com.example.cue.network.websocket

import android.content.SharedPreferences
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
import com.example.cue.utils.AppLog as Log

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

    fun sendClaudeCodeRequest(prompt: String, sessionId: String? = null, workingDirectory: String = "~"): String {
        val requestId = "req_${UUID.randomUUID()}"
        val parameters = mutableMapOf<String, Any>(
            "prompt" to prompt,
            "working_directory" to workingDirectory,
        )
        sessionId?.let { parameters["session_id"] = it }

        // Create the agent control payload directly in the format expected by backend
        val agentControlJson = mapOf(
            "type" to "agent_control",
            "payload" to mapOf(
                "control_type" to "claude_code_execute",
                "parameters" to parameters,
                "timestamp" to System.currentTimeMillis().toString(),
                "sender" to environment.clientId,
                "recipient" to "all",
            ),
            "client_id" to environment.clientId,
            "websocket_request_id" to requestId,
        )

        // Send as raw JSON
        val json = moshi.adapter<Map<String, Any>>(Map::class.java).toJson(agentControlJson)

        Log.d(TAG, "Sending Claude Code JSON: $json")

        val success = webSocket?.send(json) ?: false

        Log.d(TAG, "Claude Code request sent: $prompt (success: $success, requestId: $requestId)")
        return requestId
    }

    fun sendCueCLIRequest(message: String, sessionId: String, workingDirectory: String = "~", targetClientId: String? = null): String {
        val requestId = "req_${UUID.randomUUID()}"
        val parameters = mutableMapOf<String, Any>(
            "message" to message,
            "working_directory" to workingDirectory,
            "session_id" to sessionId,
        )

        // Create the agent control payload for Cue CLI
        val agentControlJson = mapOf(
            "type" to "agent_control",
            "payload" to mapOf(
                "control_type" to "cue_cli_execute",
                "parameters" to parameters,
                "timestamp" to System.currentTimeMillis().toString(),
                "sender" to environment.clientId,
                "recipient" to (targetClientId ?: "all"),
                "target_client_id" to targetClientId,
            ),
            "client_id" to environment.clientId,
            "websocket_request_id" to requestId,
        )

        // Send as raw JSON
        val json = moshi.adapter<Map<String, Any>>(Map::class.java).toJson(agentControlJson)

        Log.d(TAG, "Sending Cue CLI JSON to ${targetClientId ?: "all"}: $json")

        val success = webSocket?.send(json) ?: false

        Log.d(TAG, "Cue CLI request sent: $message (target: ${targetClientId ?: "all"}, success: $success, requestId: $requestId)")
        return requestId
    }

    fun createSession(sessionId: String, workingDirectory: String = "~"): String {
        val requestId = "req_${UUID.randomUUID()}"
        val parameters = mapOf<String, Any>(
            "session_id" to sessionId,
            "working_directory" to workingDirectory,
        )

        // Create the agent control payload directly in the format expected by backend
        val agentControlJson = mapOf(
            "type" to "agent_control",
            "payload" to mapOf(
                "control_type" to "create_session",
                "parameters" to parameters,
                "timestamp" to System.currentTimeMillis().toString(),
                "sender" to environment.clientId,
                "recipient" to "all",
            ),
            "client_id" to environment.clientId,
            "websocket_request_id" to requestId,
        )

        // Send as raw JSON
        val json = moshi.adapter<Map<String, Any>>(Map::class.java).toJson(agentControlJson)
        webSocket?.send(json)

        Log.d(TAG, "Sent session creation request: $sessionId")
        return requestId
    }

    fun requestClientsList(): String {
        val requestId = "req_${UUID.randomUUID()}"

        // Send ping message to request clients list (following core WebSocketClient pattern)
        val pingJson = mapOf(
            "type" to "ping",
            "payload" to mapOf(
                "type" to "request_clients",
                "message" to "Requesting client list",
                "client_id" to environment.clientId,
                "recipient" to "all",
            ),
            "client_id" to environment.clientId,
            "websocket_request_id" to requestId,
        )

        // Send as raw JSON
        val json = moshi.adapter<Map<String, Any>>(Map::class.java).toJson(pingJson)
        val success = webSocket?.send(json) ?: false

        Log.d(TAG, "Sent client list request (success: $success, requestId: $requestId)")
        return requestId
    }

    fun getClientId(): String = environment.clientId

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            scope.launch {
                _connectionState.value = ConnectionState.Connected
                retryAttempt = 0
                startPingTimer()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // Log only first 500 chars to avoid huge logs
            val logText = if (text.length > 500) text.substring(0, 500) + "..." else text
            Log.d(TAG, "Received raw message: $logText")
            scope.launch {
                try {
                    val message = eventMessageAdapter.fromJson(text)
                    Log.d(TAG, "Parsed message type: ${message?.type}, clientId: ${message?.clientId}")
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

                        EventMessageType.PING -> {
                            // Handle ping requests for client discovery
                            Log.d(TAG, "Received ping message: ${message.payload}")
                            handlePingMessage(message)
                            // Also emit the ping event so ViewModel can see client_info responses
                            _events.value = message
                        }

                        EventMessageType.AGENT_CONTROL -> {
                            Log.d(TAG, "Received agent control event from client: ${message.clientId}")
                            Log.d(TAG, "Agent control payload type: ${message.payload?.javaClass?.simpleName}")
                            _events.value = message
                        }

                        EventMessageType.TASK_STATUS -> {
                            Log.d(TAG, "Received task status event: $message")
                            _events.value = message
                        }

                        EventMessageType.MESSAGE -> {
                            Log.d(TAG, "Received message event: $message")
                            _events.value = message
                        }

                        EventMessageType.MESSAGE_CHUNK -> {
                            Log.d(TAG, "Received message chunk event")
                            _events.value = message
                        }

                        null -> {
                            Log.w(TAG, "Received message with null type, ignoring: $text")
                        }

                        else -> {
                            _events.value = message
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode message: ${e.message}", e)
                    // Log the raw message for debugging
                    Log.d(TAG, "Raw message that failed to decode: $text")
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

    private fun handlePingMessage(message: EventMessage) {
        val payload = message.payload as? Map<*, *> ?: return
        val payloadType = payload["type"] as? String
        val requestingClientId = message.clientId ?: payload["client_id"] as? String

        Log.d(TAG, "Handling ping - type: $payloadType, from: $requestingClientId, our client: ${environment.clientId}")

        when (payloadType) {
            "request_clients" -> {
                // Another client is requesting client list, respond with our info
                if (requestingClientId != environment.clientId) {
                    Log.d(TAG, "Responding to request_clients from $requestingClientId")

                    val responseJson = mapOf(
                        "type" to "ping",
                        "payload" to mapOf(
                            "type" to "client_info",
                            "message" to "Client info response",
                            "client_id" to environment.clientId,
                            "platform" to "android",
                            "short_name" to "android-${environment.clientId.take(8)}",
                            "recipient" to "all",
                        ),
                        "client_id" to environment.clientId,
                    )

                    val json = moshi.adapter<Map<String, Any>>(Map::class.java).toJson(responseJson)
                    webSocket?.send(json)
                    Log.d(TAG, "Sent client_info response")
                }
            }

            "client_info" -> {
                // Received client info response - emit as event for discovery
                if (requestingClientId != environment.clientId) {
                    Log.d(TAG, "Received client_info from $requestingClientId")
                    _events.value = message
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

    private fun calculateRetryDelay(): Long = min(
        INITIAL_RETRY_DELAY * 2.0.pow(retryAttempt.toDouble()).toLong(),
        MAX_RETRY_DELAY,
    )

    private fun startPingTimer() {
        stopPingTimer()
        pingTimer = Timer().apply {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        val pingMessage = EventMessage(
                            type = EventMessageType.PING,
                            payload = PingPongEventPayload(
                                type = "ping",
                                message = "ping",
                                sender = "client",
                            ),
                            clientId = null,
                            metadata = null,
                            websocketRequestId = null,
                        )
                        send(pingMessage)
                    }
                },
                PING_INTERVAL,
                PING_INTERVAL,
            )
        }
    }

    private fun stopPingTimer() {
        pingTimer?.cancel()
        pingTimer = null
    }
}
