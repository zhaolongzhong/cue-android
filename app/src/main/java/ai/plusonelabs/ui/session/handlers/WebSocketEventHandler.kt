package ai.plusonelabs.ui.session.handlers

import ai.plusonelabs.network.websocket.EventMessage
import ai.plusonelabs.ui.session.managers.ClientManager
import ai.plusonelabs.ui.session.managers.MessageManager

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
