package ai.plusonelabs.ui.session.handlers

import ai.plusonelabs.network.websocket.EventMessage
import ai.plusonelabs.network.websocket.EventMessageType
import ai.plusonelabs.ui.session.managers.ClientManager
import ai.plusonelabs.ui.session.managers.MessageManager
import ai.plusonelabs.utils.AppLog as Log

class ClientEventHandler : WebSocketEventHandler {
    companion object {
        private const val TAG = "ClientEventHandler"
    }

    override fun canHandle(event: EventMessage): Boolean = event.type in listOf(
        EventMessageType.CLIENT_CONNECT,
        EventMessageType.CLIENT_DISCONNECT,
        EventMessageType.PING,
    )

    override suspend fun handle(
        event: EventMessage,
        clientManager: ClientManager,
        messageManager: MessageManager,
        context: HandlerContext,
    ) {
        when (event.type) {
            EventMessageType.CLIENT_CONNECT -> handleClientConnect(event, clientManager)
            EventMessageType.CLIENT_DISCONNECT -> handleClientDisconnect(event, clientManager)
            EventMessageType.PING -> handlePing(event, clientManager, context)
            else -> Log.w(TAG, "Unexpected event type: ${event.type}")
        }
    }

    private fun handleClientConnect(event: EventMessage, clientManager: ClientManager) {
        val payload = event.payload as? Map<*, *> ?: return
        val clientId = payload["client_id"] as? String ?: return

        val client = clientManager.addOrUpdateClient(
            clientId = clientId,
            shortName = payload["short_name"] as? String,
            platform = payload["platform"] as? String,
            hostname = payload["hostname"] as? String,
            cwd = payload["cwd"] as? String,
        )

        // Auto-select if needed
        client?.let {
            if (clientManager.shouldAutoSelectClient(clientId)) {
                clientManager.selectClient(clientId)
                Log.d(TAG, "Auto-selected first CLI client: $clientId")
            }
        }
    }

    private fun handleClientDisconnect(event: EventMessage, clientManager: ClientManager) {
        val payload = event.payload as? Map<*, *> ?: return
        val clientId = payload["client_id"] as? String ?: return
        clientManager.removeClient(clientId)
    }

    private fun handlePing(event: EventMessage, clientManager: ClientManager, context: HandlerContext) {
        val payload = event.payload as? Map<*, *> ?: return
        val payloadType = payload["type"] as? String
        val clientId = event.clientId ?: payload["client_id"] as? String

        Log.d(TAG, "Received ping event - type: $payloadType, from: $clientId")

        when (payloadType) {
            "client_info" -> {
                // Handle client info response from discovery
                if (clientId != null && clientId != context.clientId) {
                    val client = clientManager.addOrUpdateClient(
                        clientId = clientId,
                        shortName = payload["short_name"] as? String,
                        platform = payload["platform"] as? String,
                        hostname = payload["hostname"] as? String,
                        cwd = payload["cwd"] as? String,
                    )

                    // Auto-select if needed
                    client?.let {
                        if (clientManager.shouldAutoSelectClient(clientId)) {
                            clientManager.selectClient(clientId)
                            Log.d(TAG, "Auto-selected first CLI client: $clientId")
                        }
                    }
                }
            }
        }
    }
}
