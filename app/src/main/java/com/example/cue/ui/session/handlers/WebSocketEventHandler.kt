package com.example.cue.ui.session.handlers

import com.example.cue.network.websocket.EventMessage
import com.example.cue.ui.session.managers.ClientManager
import com.example.cue.ui.session.managers.MessageManager

interface WebSocketEventHandler {
    fun canHandle(event: EventMessage): Boolean
    suspend fun handle(
        event: EventMessage,
        clientManager: ClientManager,
        messageManager: MessageManager,
        context: HandlerContext,
    )
}

data class HandlerContext(
    val currentSessionId: String? = null,
    val claudeCodeSessionId: String? = null,
    val clientId: String,
    val onSessionCreated: (String) -> Unit = {},
    val onSessionDestroyed: () -> Unit = {},
    val onClaudeSessionUpdate: (String) -> Unit = {},
    val onLoadingChanged: ((Boolean) -> Unit)? = null,
)
