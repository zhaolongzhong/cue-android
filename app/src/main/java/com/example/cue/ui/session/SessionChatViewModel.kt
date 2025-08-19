package com.example.cue.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.Environment
import com.example.cue.network.websocket.AgentControlPayload
import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.EventMessageType
import com.example.cue.network.websocket.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.cue.utils.AppLog as Log

@HiltViewModel
class SessionChatViewModel @Inject constructor(
    private val webSocketService: WebSocketService,
    private val environment: Environment,
) : ViewModel() {

    companion object {
        private const val TAG = "SessionChatViewModel"
    }

    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val content: String,
        val isFromUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val sessionId: String? = null,
    )

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private var claudeCodeSessionId: String? = null
    private val pendingRequestIds = mutableSetOf<String>()

    init {
        // Create a new session on init
        createNewSession()

        // Listen to WebSocket events
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                event?.let { handleWebSocketEvent(it) }
            }
        }
    }

    fun createNewSession() {
        val newSessionId = "android-session-${UUID.randomUUID().toString().take(8)}"
        _sessionId.value = newSessionId

        // Send session creation request
        val requestId = webSocketService.createSession(newSessionId)
        pendingRequestIds.add(requestId)

        // Add welcome message
        addMessage(
            ChatMessage(
                content = "Session created: $newSessionId\nYou can now send messages or use /claude commands",
                isFromUser = false,
                sessionId = newSessionId,
            ),
        )

        Log.d(TAG, "Created new session: $newSessionId")
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        Log.d(TAG, "sendMessage called with: '$text'")

        // Add user message
        addMessage(
            ChatMessage(
                content = text,
                isFromUser = true,
                sessionId = _sessionId.value,
            ),
        )

        // Check if it's a Claude Code command
        when {
            text.startsWith("/claude ") || text.startsWith("/cc ") -> {
                Log.d(TAG, "Detected Claude Code command: $text")
                val prompt = text
                    .replace("/claude ", "")
                    .replace("/cc ", "")
                    .trim()
                Log.d(TAG, "Extracted prompt: '$prompt'")
                sendClaudeCodeRequest(prompt)
            }
            text == "/new" -> {
                Log.d(TAG, "Creating new session")
                createNewSession()
            }
            text == "/clear" -> {
                Log.d(TAG, "Clearing messages")
                _messages.value = emptyList()
                addMessage(
                    ChatMessage(
                        content = "Chat cleared",
                        isFromUser = false,
                        sessionId = _sessionId.value,
                    ),
                )
            }
            else -> {
                Log.d(TAG, "Regular message, echoing: $text")
                // For now, just echo back regular messages
                addMessage(
                    ChatMessage(
                        content = "Echo: $text",
                        isFromUser = false,
                        sessionId = _sessionId.value,
                    ),
                )
            }
        }
    }

    private fun sendClaudeCodeRequest(prompt: String) {
        Log.d(TAG, "sendClaudeCodeRequest called with prompt: '$prompt'")
        _isLoading.value = true

        // Add status message
        addMessage(
            ChatMessage(
                content = "🧠 Executing Claude Code...",
                isFromUser = false,
                sessionId = _sessionId.value,
            ),
        )

        Log.d(TAG, "Current claudeCodeSessionId: $claudeCodeSessionId")

        // Send Claude Code request
        val requestId = webSocketService.sendClaudeCodeRequest(
            prompt = prompt,
            sessionId = claudeCodeSessionId,
            workingDirectory = "/Users",
        )
        pendingRequestIds.add(requestId)

        Log.d(TAG, "Claude Code request sent - prompt: '$prompt', requestId: $requestId, pendingRequests: ${pendingRequestIds.size}")
    }

    private fun handleWebSocketEvent(event: EventMessage) {
        when (event.type) {
            EventMessageType.AGENT_CONTROL -> {
                handleAgentControlEvent(event)
            }
            EventMessageType.TASK_STATUS -> {
                handleTaskStatusEvent(event)
            }
            EventMessageType.MESSAGE -> {
                // Handle regular messages if needed
                Log.d(TAG, "Received message event: ${(event.payload as? com.example.cue.network.websocket.MessagePayload)?.message}")
            }
            else -> {
                // Handle other event types if needed
            }
        }
    }

    private fun handleTaskStatusEvent(event: EventMessage) {
        // Parse task status payload
        val payload = when (val p = event.payload) {
            is com.example.cue.network.websocket.TaskStatusEventPayload -> p
            is Map<*, *> -> {
                // Handle when payload comes as a Map
                @Suppress("UNCHECKED_CAST")
                val payloadMap = p as? Map<String, Any>
                val sessionId = payloadMap?.get("session_id") as? String ?: ""
                val message = payloadMap?.get("message") as? String
                val status = payloadMap?.get("status") as? String
                val step = payloadMap?.get("step") as? String
                val requestId = payloadMap?.get("request_id") as? String

                Log.d(TAG, "Task Status - sessionId: $sessionId, status: $status, step: $step, message: $message")

                // Display status update
                message?.let { statusMessage ->
                    val statusIcon = when (status) {
                        "received" -> "📥"
                        "validated" -> "✅"
                        "executing" -> "⚙️"
                        "completed" -> "✔️"
                        "error" -> "❌"
                        else -> "📋"
                    }

                    val formattedMessage = "$statusIcon $statusMessage"

                    // Find the last status message (starts with 🧠)
                    val messages = _messages.value.toMutableList()
                    val lastStatusIndex = messages.indexOfLast { it.content.startsWith("🧠 Executing Claude Code") }

                    if (lastStatusIndex != -1 && status != "error") {
                        // Update existing status message with new progress
                        val existingMessage = messages[lastStatusIndex]
                        val existingContent = existingMessage.content

                        // Build progressive status content
                        val newContent = if (status == "completed") {
                            // For completed status, keep all previous steps and add completion
                            "$existingContent\n$formattedMessage"
                        } else {
                            // For ongoing status, add the new step
                            if (existingContent.contains("\n")) {
                                // Already has status updates, append new one
                                "$existingContent\n$formattedMessage"
                            } else {
                                // First status update
                                "$existingContent\n$formattedMessage"
                            }
                        }

                        messages[lastStatusIndex] = existingMessage.copy(content = newContent)
                        _messages.value = messages

                        // Set loading to false when completed
                        if (status == "completed" || status == "error") {
                            _isLoading.value = false
                        }
                    } else if (status == "error") {
                        // Add error as separate message
                        addMessage(
                            ChatMessage(
                                content = formattedMessage,
                                isFromUser = false,
                                sessionId = _sessionId.value,
                            ),
                        )
                        _isLoading.value = false
                    }
                }

                null // Return null since we handled it inline
            }
            else -> {
                Log.d(TAG, "Unknown task status payload type: ${p?.javaClass}")
                null
            }
        }
    }

    private fun handleAgentControlEvent(event: EventMessage) {
        val requestId = event.websocketRequestId
        Log.d(TAG, "handleAgentControlEvent - requestId: $requestId, clientId: ${event.clientId}, our pendingRequests: $pendingRequestIds")

        // Skip our own messages (echoed back from server)
        if (event.clientId == environment.clientId) {
            Log.d(TAG, "Ignoring our own echoed message")
            return
        }

        // For responses, check if it's for one of our requests
        // But for claude_code_response, we should always process it
        // (it might not have the same requestId)

        // Parse the agent control payload - the payload can be either AgentControlPayload or a Map
        val controlType: String?
        val parameters: Map<String, Any>?

        Log.d(TAG, "Parsing agent control payload: ${event.payload}")

        when (val payload = event.payload) {
            is com.example.cue.network.websocket.AgentControlPayload -> {
                Log.d(TAG, "Payload is AgentControlPayload")
                controlType = payload.controlType
                parameters = payload.parameters
            }
            is Map<*, *> -> {
                Log.d(TAG, "Payload is Map")
                // Handle when payload comes as a Map (from backend JSON parsing)
                @Suppress("UNCHECKED_CAST")
                val payloadMap = payload as? Map<String, Any>
                controlType = payloadMap?.get("control_type") as? String
                parameters = payloadMap?.get("parameters") as? Map<String, Any>
                Log.d(TAG, "Extracted from map - controlType: $controlType, parameters: $parameters")
            }
            else -> {
                Log.d(TAG, "Unknown payload type: ${payload?.javaClass}")
                controlType = null
                parameters = null
            }
        }

        Log.d(TAG, "Parsed control type: '$controlType', parameters: $parameters")

        when (controlType) {
            "claude_code_response" -> {
                Log.d(TAG, "Handling Claude Code response from CLI: ${event.clientId}")
                _isLoading.value = false

                // Remove the request ID if it exists
                if (requestId != null) {
                    pendingRequestIds.remove(requestId)
                }

                val sessionId = parameters?.get("session_id") as? String
                val result = parameters?.get("result") as? String
                val error = parameters?.get("error") as? String
                val projectPath = parameters?.get("project_path") as? String

                Log.d(TAG, "Claude response - sessionId: $sessionId, result: ${result?.take(100)}, error: $error, from CLI: ${event.clientId}")

                // Store Claude Code session ID
                sessionId?.let { claudeCodeSessionId = it }

                // Display response
                if (!error.isNullOrEmpty()) {
                    addMessage(
                        ChatMessage(
                            content = "❌ Claude Code Error: $error",
                            isFromUser = false,
                            sessionId = _sessionId.value,
                        ),
                    )
                } else if (!result.isNullOrEmpty()) {
                    val responseContent = buildString {
                        appendLine("🤖 Claude Code Response")
                        sessionId?.let { appendLine("Session: $it") }
                        projectPath?.let { appendLine("Project: $it") }
                        appendLine()
                        append(result)
                    }

                    addMessage(
                        ChatMessage(
                            content = responseContent,
                            isFromUser = false,
                            sessionId = _sessionId.value,
                        ),
                    )
                }

                Log.d(TAG, "Received Claude Code response - Session: $sessionId, Result: ${result?.take(100)}")
            }
            "create_session" -> {
                // Session creation confirmation
                pendingRequestIds.remove(requestId)
                Log.d(TAG, "Session creation confirmed")
            }
            else -> {
                Log.d(TAG, "Received unknown agent control type: $controlType")
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun connectWebSocket() {
        webSocketService.connect()
    }

    fun disconnectWebSocket() {
        webSocketService.disconnect()
    }
}
