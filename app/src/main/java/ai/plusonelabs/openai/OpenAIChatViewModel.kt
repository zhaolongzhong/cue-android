package ai.plusonelabs.openai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val chatService: OpenAIChatService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenAIChatUiState())
    val uiState: StateFlow<OpenAIChatUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentInput = _uiState.value.inputText.trim()
        if (currentInput.isEmpty() || _uiState.value.isLoading) return

        val userMessage = Message(content = currentInput, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val response = chatService.sendMessage(currentInput)
                val assistantMessage = Message(content = response, isUser = false)
                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false,
                    )
                }
            } catch (e: ChatError) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
