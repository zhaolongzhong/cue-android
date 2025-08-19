package com.example.cue.ui.session.handlers

import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.EventMessageType
import com.example.cue.ui.session.CLIStatus
import com.example.cue.ui.session.managers.ClientManager
import com.example.cue.ui.session.managers.MessageManager
import com.example.cue.utils.AppLog as Log

class SessionEventHandler : WebSocketEventHandler {
    companion object {
        private const val TAG = "SessionEventHandler"
    }

    override fun canHandle(event: EventMessage): Boolean = event.type == EventMessageType.SESSION

    override suspend fun handle(
        event: EventMessage,
        clientManager: ClientManager,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        val payload = event.payload as? Map<*, *> ?: return
        val action = payload["action"] as? String
        val sessionId = payload["session_id"] as? String
        val sessionInfo = payload["session_info"] as? Map<*, *>

        Log.d(TAG, "Received session event - action: $action, sessionId: $sessionId")

        when (action) {
            "create" -> handleSessionCreate(sessionInfo, messageManager, context)
            "update" -> Log.d(TAG, "Session updated: $sessionId")
            "destroy" -> handleSessionDestroy(sessionId, context)
            else -> Log.d(TAG, "Unknown session action: $action")
        }
    }

    private fun handleSessionCreate(
        sessionInfo: Map<*, *>?,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        sessionInfo?.let {
            val cliSessionId = it["sessionId"] as? String
            val clientId = it["clientId"] as? String
            val currentModel = it["currentModel"] as? String

            // Notify about the created session
            cliSessionId?.let { id ->
                context.onClaudeSessionUpdate(id)
            }

            messageManager.addStatusMessage(
                content = "✅ Session created with CLI client: $clientId\nModel: $currentModel",
                status = CLIStatus.COMPLETED,
                sessionId = context.currentSessionId,
            )

            Log.d(TAG, "Session created successfully - sessionId: $cliSessionId, clientId: $clientId")
        }
    }

    private fun handleSessionDestroy(sessionId: String?, context: HandlerContext) {
        context.onSessionDestroyed()
        Log.d(TAG, "Session destroyed: $sessionId")
    }
}
