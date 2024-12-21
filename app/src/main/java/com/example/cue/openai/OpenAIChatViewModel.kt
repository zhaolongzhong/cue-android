package com.example.cue.openai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.BuildConfig
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Message(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

data class OpenAIChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OpenAIChatViewModel @Inject constructor(
    private val networkClient: NetworkClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenAIChatUiState())
    val uiState: StateFlow<OpenAIChatUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentInput = _uiState.value.inputText.trim()
        if (currentInput.isEmpty() || _uiState.value.isLoading) return

        // Add user message
        val userMessage = Message(content = currentInput, isUser = true)
        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val response = networkClient.post<OpenAIResponse>(
                    endpoint = "chat/completions",
                    body = mapOf(
                        "model" to "gpt-3.5-turbo",
                        "messages" to listOf(
                            mapOf(
                                "role" to "user",
                                "content" to currentInput
                            )
                        )
                    ),
                    responseType = OpenAIResponse::class.java
                )

                val assistantMessage = Message(
                    content = response.choices.firstOrNull()?.message?.content ?: "No response",
                    isUser = false
                )

                _uiState.update { 
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }
            } catch (e: NetworkError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// Data classes for OpenAI API response
data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>,
)

data class Choice(
    val message: ChatMessage,
)

data class ChatMessage(
    val role: String,
    val content: String,
)