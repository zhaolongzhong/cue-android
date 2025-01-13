package com.example.cue.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.AssistantRepository
import com.example.cue.chat.models.ConversationModel
import com.example.cue.chat.models.MessageModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var primaryConversation: ConversationModel? = null
    private var messageCount = 0

    fun setupChat(assistantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get or create primary conversation
                val primaryConversations = assistantRepository.listAssistantConversations(
                    assistantId = assistantId,
                    isPrimary = true,
                    limit = 1,
                ).getOrThrow()

                primaryConversation = if (primaryConversations.isEmpty()) {
                    assistantRepository.createPrimaryConversation(assistantId).getOrThrow()
                } else {
                    primaryConversations[0]
                }

                // Load initial messages
                loadMessages(primaryConversation!!.id)
            } catch (e: Exception) {
                _error.value = "Failed to setup chat: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadMessages(conversationId: String) {
        try {
            val messages = assistantRepository.listMessages(
                conversationId = conversationId,
            ).getOrThrow()

            _messages.value = messages
            messageCount = messages.size
        } catch (e: Exception) {
            _error.value = "Failed to load messages: ${e.message}"
        }
    }

    fun loadMoreMessages() {
        if (_isLoading.value) return
        val conversation = primaryConversation ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newMessages = assistantRepository.listMessages(
                    conversationId = conversation.id,
                    skip = messageCount,
                    limit = 20,
                ).getOrThrow()

                if (newMessages.isNotEmpty()) {
                    val existingMessageIds = _messages.value.map { it.id }.toSet()
                    val uniqueNewMessages = newMessages.filterNot { it.id in existingMessageIds }

                    if (uniqueNewMessages.isNotEmpty()) {
                        _messages.value = _messages.value + uniqueNewMessages
                        messageCount += uniqueNewMessages.size
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load more messages: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
