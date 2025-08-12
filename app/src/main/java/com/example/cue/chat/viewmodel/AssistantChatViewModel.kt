package com.example.cue.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.ClientStatusService
import com.example.cue.auth.AuthService
import com.example.cue.network.websocket.ConnectionState
import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.EventMessageType
import com.example.cue.network.websocket.MessagePayload
import com.example.cue.network.websocket.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.cue.utils.AppLog as Log

@HiltViewModel
class AssistantChatViewModel @Inject constructor(
    private val webSocketService: WebSocketService,
    private val clientStatusService: ClientStatusService,
    private val authService: AuthService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val TAG = "AssistantChatViewModel"
    }

    private val assistantId: String = checkNotNull(savedStateHandle["assistantId"])
    private val clientStatus = clientStatusService.getClientStatus(assistantId)
    private val userId: String by lazy {
        authService.currentUser.value?.id ?: ""
    }

    data class ChatUiState(
        val messages: List<Message> = emptyList(),
        val inputText: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
        val connectionState: ConnectionState = ConnectionState.Disconnected,
    )

    data class Message(
        val content: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeWebSocketEvents()
        observeConnectionState()
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events
                .filterNotNull()
                .collect { event ->
                    when (event.type) {
                        EventMessageType.ASSISTANT -> event.payload?.let { handleAssistantMessage(it) }
                        EventMessageType.USER -> event.payload?.let { handleUserMessage(it) }
                        else -> Unit // Ignore other events
                    }
                }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketService.connectionState.collect { state ->
                Log.d(TAG, "Update to connection state: $state")
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }

    private fun handleAssistantMessage(payload: MessagePayload) {
        payload.message?.let { content ->
            val message = Message(
                content = content,
                isUser = false,
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + message,
                    isLoading = false,
                )
            }
        }
    }

    private fun handleUserMessage(payload: MessagePayload) {
        payload.message?.let { content ->
            val message = Message(
                content = content,
                isUser = true,
            )
            _uiState.update {
                it.copy(
                    messages = it.messages + message,
                )
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentInput = _uiState.value.inputText.trim()
        if (currentInput.isEmpty() || _uiState.value.isLoading) return

        val recipientId = clientStatus?.runnerId ?: assistantId
        if (userId.isEmpty()) {
            Log.e(TAG, "userId is empty.")
            return
        }

        val messagePayload = MessagePayload(
            message = currentInput,
            sender = userId,
            recipient = recipientId,
            websocketRequestId = UUID.randomUUID().toString(),
            metadata = null,
            userId = userId,
            msgId = null,
            payload = null,
        )

        val eventMessage = EventMessage(
            type = EventMessageType.USER,
            payload = messagePayload,
            clientId = null,
            metadata = null,
            websocketRequestId = null,
        )

        webSocketService.send(eventMessage)
        _uiState.update { it.copy(inputText = "", isLoading = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
