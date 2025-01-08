package com.example.cue.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.service.AssistantService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantListViewModel @Inject constructor(
    private val assistantService: AssistantService
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Error(
            val message: String,
            val type: ErrorType? = null
        ) : UiState()
        data class Success(
            val assistants: List<Assistant>,
            val isRefreshing: Boolean = false
        ) : UiState()
    }

    enum class ErrorType {
        NETWORK,
        PERMISSION_DENIED,
        UNKNOWN
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private fun handleError(e: Exception): ErrorType {
        return when {
            e.message?.contains("network", ignoreCase = true) == true -> ErrorType.NETWORK
            e.message?.contains("permission", ignoreCase = true) == true -> ErrorType.PERMISSION_DENIED
            else -> ErrorType.UNKNOWN
        }
    }

    init {
        loadAssistants()
    }

    fun loadAssistants() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val assistants = assistantService.getAssistants()
                _uiState.value = UiState.Success(assistants)
            } catch (e: Exception) {
                val errorType = handleError(e)
                _uiState.value = UiState.Error(
                    message = when (errorType) {
                        ErrorType.NETWORK -> "Network error. Please check your connection."
                        ErrorType.PERMISSION_DENIED -> "You don't have permission to view assistants."
                        ErrorType.UNKNOWN -> e.message ?: "Failed to load assistants"
                    },
                    type = errorType
                )
            }
        }
    }

    fun refreshAssistants() {
        viewModelScope.launch {
            try {
                // Only show refreshing if we already have data
                if (_uiState.value is UiState.Success) {
                    _uiState.value = UiState.Success(
                        assistants = ((_uiState.value as UiState.Success).assistants),
                        isRefreshing = true
                    )
                }
                
                val assistants = assistantService.getAssistants()
                _uiState.value = UiState.Success(assistants)
            } catch (e: Exception) {
                val errorType = handleError(e)
                _uiState.value = UiState.Error(
                    message = when (errorType) {
                        ErrorType.NETWORK -> "Network error while refreshing. Please try again."
                        ErrorType.PERMISSION_DENIED -> "You don't have permission to view assistants."
                        ErrorType.UNKNOWN -> e.message ?: "Failed to refresh assistants"
                    },
                    type = errorType
                )
            }
        }
    }
}