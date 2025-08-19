package com.example.cue.ui.session.handlers

import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.EventMessageType
import com.example.cue.ui.session.CLIStatus
import com.example.cue.ui.session.managers.ClientManager
import com.example.cue.ui.session.managers.MessageManager
import com.example.cue.utils.AppLog as Log

class TaskStatusEventHandler : WebSocketEventHandler {
    companion object {
        private const val TAG = "TaskStatusEventHandler"
    }

    override fun canHandle(event: EventMessage): Boolean = event.type == EventMessageType.TASK_STATUS

    override suspend fun handle(
        event: EventMessage,
        clientManager: ClientManager,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        // Parse task status payload
        when (val payload = event.payload) {
            is com.example.cue.network.websocket.TaskStatusEventPayload -> {
                handleTaskStatus(payload, messageManager, context)
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val payloadMap = payload as? Map<String, Any>
                handleTaskStatusFromMap(payloadMap, messageManager, context)
            }
            else -> {
                Log.d(TAG, "Unknown task status payload type: ${payload?.javaClass}")
            }
        }
    }

    private fun handleTaskStatus(
        payload: com.example.cue.network.websocket.TaskStatusEventPayload,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        Log.d(TAG, "Task Status - sessionId: ${payload.sessionId}, message: ${payload.message}")

        payload.message?.let { message ->
            messageManager.addStatusMessage(
                content = message,
                status = CLIStatus.EXECUTING,
                sessionId = context.currentSessionId,
            )
        }
    }

    private fun handleTaskStatusFromMap(
        payloadMap: Map<String, Any>?,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        val sessionId = payloadMap?.get("session_id") as? String ?: ""
        val message = payloadMap?.get("message") as? String
        val status = payloadMap?.get("status") as? String
        val step = payloadMap?.get("step") as? String
        val messageId = payloadMap?.get("message_id") as? String

        Log.d(TAG, "Task Status - sessionId: $sessionId, status: $status, step: $step, message: $message, messageId: $messageId")

        // Display status update
        message?.let { statusMessage ->
            val cliStatus = when (status) {
                "received" -> CLIStatus.RECEIVED
                "validated" -> CLIStatus.VALIDATED
                "executing" -> CLIStatus.EXECUTING
                "completed" -> CLIStatus.COMPLETED
                "error" -> CLIStatus.ERROR
                else -> CLIStatus.EXECUTING
            }

            if (cliStatus == CLIStatus.ERROR) {
                messageManager.addErrorMessage(
                    content = statusMessage,
                    sessionId = context.currentSessionId,
                )
                // Stop loading on error
                context.onLoadingChanged?.invoke(false)
            } else {
                // Use message ID for streaming if available, otherwise fall back to last message update
                if (!messageId.isNullOrBlank()) {
                    messageManager.updateOrCreateMessageByStreamId(
                        messageId = messageId,
                        content = statusMessage,
                        status = cliStatus,
                        sessionId = context.currentSessionId,
                    )
                } else {
                    messageManager.updateLastExecutingMessage(statusMessage, cliStatus)
                }

                // Stop loading on completion
                if (cliStatus == CLIStatus.COMPLETED) {
                    context.onLoadingChanged?.invoke(false)
                }
            }
        }
    }
}
