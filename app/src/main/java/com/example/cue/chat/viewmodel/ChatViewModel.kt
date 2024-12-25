package com.example.cue.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistants.models.Assistant
import com.example.cue.chat.models.ChatError
import com.example.cue.chat.models.ConversationModel
import com.example.cue.chat.models.MessageModel
import com.example.cue.chat.models.MessagePayload
import com.example.cue.chat.repository.MessageRepository
import com.example.cue.network.ClientEvent
import com.example.cue.network.ClientStatus
import com.example.cue.network.ConnectionState
import com.example.cue.network.WebSocketService
import com.example.cue.utils.EnvironmentConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageModel> = emptyList(),
    val isLoading: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val error: ChatError? = null,
    val inputEnabled: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val webSocketService: WebSocketService,
    private val messageRepository: MessageRepository,
    private val assistantRepository: AssistantRepository,
    private val clientStatusService: ClientStatusService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var _assistant = MutableStateFlow<Assistant?>(null)
    val assistant: StateFlow<Assistant?> = _assistant.asStateFlow()

    private var _inputMessage = MutableStateFlow("")
    val inputMessage: StateFlow<String> = _inputMessage.asStateFlow()

    private var primaryConversation: ConversationModel? = null
    private var messageSubscription: Job? = null

    init {
        viewModelScope.launch {
            webSocketService.connectionState
                .onEach { connectionState ->
                    _uiState.update { it.copy(
                        connectionState = connectionState,
                        inputEnabled = connectionState == ConnectionState.Connected
                    )}
                }
                .launchIn(this)

            webSocketService.messages
                .filterIsInstance<MessagePayload>()
                .onEach { messagePayload -> 
                    processMessagePayload(messagePayload)
                }
                .launchIn(this)
        }
    }

    fun initialize(assistant: Assistant) {
        _assistant.value = assistant
        viewModelScope.launch {
            setupChat()
        }
    }

    private suspend fun setupChat() {
        _uiState.update { it.copy(isLoading = true) }
        
        try {
            primaryConversation = fetchAssistantConversation(_assistant.value?.id)
            
            primaryConversation?.id?.let { conversationId ->
                subscribeToMessages(conversationId)
                val messages = fetchMessages(conversationId)
                _uiState.update { it.copy(
                    messages = messages.sortedBy { it.createdAt }
                )}
            }
        } catch (e: Exception) {
            handleError(e, "Setting up chat")
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun fetchAssistantConversation(assistantId: String?): ConversationModel? {
        if (assistantId == null) return null
        
        return try {
            val conversations = assistantRepository.listAssistantConversations(
                id = assistantId,
                isPrimary = true,
                skip = 0,
                limit = 20
            )
            
            if (conversations.isEmpty()) {
                createPrimaryConversation(assistantId)
            } else {
                conversations[0]
            }
        } catch (e: Exception) {
            handleError(e, "Fetching assistant conversations")
            null
        }
    }

    private suspend fun createPrimaryConversation(assistantId: String): ConversationModel? {
        return try {
            assistantRepository.createPrimaryConversation(assistantId)
        } catch (e: Exception) {
            handleError(e, "Creating primary conversation")
            null
        }
    }

    private suspend fun fetchMessages(conversationId: String): List<MessageModel> {
        return try {
            messageRepository.listMessages(
                conversationId = conversationId,
                skip = 0,
                limit = 50
            )
        } catch (e: Exception) {
            handleError(e, "Fetching messages")
            emptyList()
        }
    }

    private fun subscribeToMessages(conversationId: String) {
        messageSubscription?.cancel()
        messageSubscription = viewModelScope.launch {
            messageRepository.messageStream(conversationId)
                .collect { message ->
                    _uiState.update { 
                        it.copy(messages = it.messages + message)
                    }
                }
        }
    }

    private suspend fun processMessagePayload(messagePayload: MessagePayload) {
        val conversationId = primaryConversation?.id ?: run {
            handleError(Exception("Conversation ID is null"), "Processing message payload")
            return
        }

        val messageModel = MessageModel(
            payload = messagePayload,
            conversationId = conversationId
        )

        try {
            messageRepository.saveMessage(messageModel)
        } catch (e: Exception) {
            handleError(e, "Saving received message")
        }
    }

    fun updateInputMessage(message: String) {
        _inputMessage.value = message
    }

    fun sendMessage() {
        val messageText = _inputMessage.value.trim()
        if (messageText.isEmpty()) return

        viewModelScope.launch {
            try {
                val assistantId = _assistant.value?.id ?: throw Exception("Assistant ID is null")
                val clientStatus = clientStatusService.getClientStatus(assistantId)
                val runnerId = clientStatus?.runnerId ?: throw Exception("Runner ID is null")

                val messagePayload = MessagePayload(
                    message = messageText,
                    recipient = runnerId,
                    websocketRequestId = UUID.randomUUID().toString()
                )

                val clientEvent = ClientEvent(
                    type = "user",
                    payload = messagePayload,
                    clientId = EnvironmentConfig.clientId
                )

                webSocketService.send(clientEvent)
                _inputMessage.value = ""
            } catch (e: Exception) {
                handleError(e, "Sending message")
            }
        }
    }

    fun updateAssistant(newAssistant: Assistant) {
        _assistant.value = newAssistant
    }

    private fun handleError(error: Throwable, context: String) {
        val chatError = when (error) {
            is WebSocketService.WebSocketException -> ChatError.ApiError(error.message ?: "WebSocket error")
            else -> ChatError.UnknownError("$context: ${error.message ?: "Unknown error"}")
        }
        _uiState.update { it.copy(error = chatError) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        messageSubscription?.cancel()
    }
}