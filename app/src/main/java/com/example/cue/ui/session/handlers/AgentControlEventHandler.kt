package com.example.cue.ui.session.handlers

import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.EventMessageType
import com.example.cue.ui.session.CLIStatus
import com.example.cue.ui.session.managers.ClientManager
import com.example.cue.ui.session.managers.MessageManager
import com.example.cue.utils.AppLog as Log

class AgentControlEventHandler : WebSocketEventHandler {
    companion object {
        private const val TAG = "AgentControlEventHandler"
    }

    private val pendingRequestIds = mutableSetOf<String>()

    override fun canHandle(event: EventMessage): Boolean = event.type == EventMessageType.AGENT_CONTROL

    override suspend fun handle(
        event: EventMessage,
        clientManager: ClientManager,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        val requestId = event.websocketRequestId
        Log.d(TAG, "handleAgentControlEvent - requestId: $requestId, clientId: ${event.clientId}")

        // Skip our own messages (echoed back from server)
        if (event.clientId == context.clientId) {
            Log.d(TAG, "Ignoring our own echoed message")
            return
        }

        // Parse the agent control payload
        val controlType: String?
        val parameters: Map<String, Any>?

        when (val payload = event.payload) {
            is com.example.cue.network.websocket.AgentControlPayload -> {
                controlType = payload.controlType
                parameters = payload.parameters
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val payloadMap = payload as? Map<String, Any>
                controlType = payloadMap?.get("control_type") as? String
                parameters = payloadMap?.get("parameters") as? Map<String, Any>
            }
            else -> {
                Log.d(TAG, "Unknown payload type: ${payload?.javaClass}")
                return
            }
        }

        Log.d(TAG, "Parsed control type: '$controlType', parameters: $parameters")

        when (controlType) {
            "claude_code_response" -> handleClaudeCodeResponse(
                parameters,
                requestId,
                event.clientId,
                messageManager,
                context,
            )
            "cue_cli_response" -> handleCueCLIResponse(
                parameters,
                requestId,
                event.clientId,
                messageManager,
                context,
            )
            "create_session" -> {
                // Session creation confirmation
                requestId?.let { pendingRequestIds.remove(it) }
                Log.d(TAG, "Session creation confirmed")
            }
            else -> {
                Log.d(TAG, "Received unknown agent control type: $controlType")
            }
        }
    }

    private fun handleClaudeCodeResponse(
        parameters: Map<String, Any>?,
        requestId: String?,
        fromClientId: String?,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        Log.d(TAG, "Handling Claude Code response from CLI: $fromClientId")

        // Remove the request ID if it exists
        requestId?.let { pendingRequestIds.remove(it) }

        val sessionId = parameters?.get("session_id") as? String
        val result = parameters?.get("result") as? String
        val error = parameters?.get("error") as? String
        val projectPath = parameters?.get("project_path") as? String

        Log.d(TAG, "Claude response - sessionId: $sessionId, result: ${result?.take(100)}, error: $error")

        // Store Claude Code session ID
        sessionId?.let { context.onClaudeSessionUpdate(it) }

        // Display response
        if (!error.isNullOrEmpty()) {
            messageManager.addErrorMessage(
                content = "❌ Claude Code Error: $error",
                sessionId = context.currentSessionId,
            )
        } else if (!result.isNullOrEmpty()) {
            val responseContent = buildString {
                appendLine("🤖 Claude Code Response")
                sessionId?.let { appendLine("Session: $it") }
                projectPath?.let { appendLine("Project: $it") }
                appendLine()
                append(result)
            }

            messageManager.addResponseMessage(
                content = responseContent,
                sessionId = context.currentSessionId,
            )
        }
    }

    private fun handleCueCLIResponse(
        parameters: Map<String, Any>?,
        requestId: String?,
        fromClientId: String?,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        Log.d(TAG, "Handling Cue CLI response from CLI: $fromClientId")

        // Remove the request ID if it exists
        requestId?.let { pendingRequestIds.remove(it) }

        val sessionId = parameters?.get("session_id") as? String
        val result = parameters?.get("result") as? String
        val message = parameters?.get("message") as? String
        val error = parameters?.get("error") as? String

        Log.d(TAG, "Cue CLI response - sessionId: $sessionId, result: ${result?.take(100)}, message: ${message?.take(100)}, error: $error")

        // Store session ID if provided
        sessionId?.let { context.onClaudeSessionUpdate(it) }

        // Display response
        if (!error.isNullOrEmpty()) {
            messageManager.addErrorMessage(
                content = "❌ Cue CLI Error: $error",
                sessionId = context.currentSessionId,
            )
            // Stop loading on error
            context.onLoadingChanged?.invoke(false)
        } else {
            val responseContent = buildString {
                appendLine("💬 Cue CLI Response")
                sessionId?.let { appendLine("Session: $it") }
                appendLine()

                // Use result if available, otherwise use message
                val content = result ?: message ?: "Response received"
                append(content)
            }

            messageManager.addResponseMessage(
                content = responseContent,
                sessionId = context.currentSessionId,
            )
            // Stop loading on success
            context.onLoadingChanged?.invoke(false)
        }
    }

    fun addPendingRequest(requestId: String) {
        pendingRequestIds.add(requestId)
    }
}
