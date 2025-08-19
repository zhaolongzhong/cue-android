package com.example.cue.assistant

import com.example.cue.assistant.models.ClientStatus
import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.EventMessageType
import com.example.cue.network.websocket.WebSocketService
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.cue.utils.AppLog as Log

@ActivityRetainedScoped
class ClientStatusService @Inject constructor(
    private val webSocketService: WebSocketService,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val TAG = "ClientStatusService"
    }

    private val scope = CoroutineScope(dispatcher + Job())
    private val _clientStatuses = MutableStateFlow<Map<String, ClientStatus>>(emptyMap())
    val clientStatuses: StateFlow<Map<String, ClientStatus>> = _clientStatuses.asStateFlow()

    init {
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        scope.launch {
            webSocketService.events
                .filterNotNull()
                .collect { event ->
                    when (event.type) {
                        EventMessageType.CLIENT_CONNECT,
                        EventMessageType.CLIENT_STATUS,
                        -> handleClientStatusMessage(event)
                        EventMessageType.CLIENT_DISCONNECT -> event.clientId?.let { markClientOffline(it) }
                        else -> Unit
                    }
                }
        }
    }

    private fun handleClientStatusMessage(message: EventMessage) {
        val messagePayload = message.payload as? com.example.cue.network.websocket.MessagePayload
        val payload = messagePayload?.payload
        Log.d(TAG, "Receive client status: $payload")
        if (payload != null) {
            val runnerId = (payload["runner_id"] as? String)
            val assistantId = (payload["assistant_id"] as? String)

            val status = ClientStatus(
                clientId = message.clientId ?: "",
                runnerId = runnerId,
                assistantId = assistantId,
                isOnline = true,
            )
            updateClientStatus(status)
        }
    }

    private fun updateClientStatus(status: ClientStatus) {
        val currentStatuses = _clientStatuses.value.toMutableMap()
        currentStatuses[status.assistantId ?: status.clientId] = status
        _clientStatuses.value = currentStatuses
    }

    private fun markClientOffline(clientId: String) {
        val currentStatuses = _clientStatuses.value.toMutableMap()
        currentStatuses[clientId]?.let { status ->
            currentStatuses[clientId] = status.copy(isOnline = false)
            _clientStatuses.value = currentStatuses
        }
    }

    fun getClientStatus(assistantId: String): ClientStatus? = _clientStatuses.value.values.find { it.assistantId == assistantId }
}
