package com.example.cue.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.models.AssistantMetadataUpdate
import com.example.cue.assistant.service.AssistantService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantDetailViewModel @Inject constructor(
    private val assistantService: AssistantService
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Error(val message: String, val type: ErrorType? = null) : UiState()
        data class Success(val assistant: Assistant) : UiState()
    }
    
    enum class ErrorType {
        NETWORK,
        NOT_FOUND,
        PERMISSION_DENIED,
        UNKNOWN
    }
    
    private fun handleError(e: Exception): ErrorType {
        return when {
            e.message?.contains("network", ignoreCase = true) == true -> ErrorType.NETWORK
            e.message?.contains("not found", ignoreCase = true) == true -> ErrorType.NOT_FOUND
            e.message?.contains("permission", ignoreCase = true) == true -> ErrorType.PERMISSION_DENIED
            else -> ErrorType.UNKNOWN
        }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadAssistant(assistantId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val assistant = assistantService.getAssistant(assistantId)
                _uiState.value = UiState.Success(assistant)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load assistant")
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value as? UiState.Success ?: return@launch
                val updatedAssistant = assistantService.updateAssistant(
                    currentState.assistant.id,
                    newName
                )
                _uiState.value = UiState.Success(updatedAssistant)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update name")
            }
        }
    }

    fun updateModel(newModel: String) {
        updateMetadata { currentMetadata ->
            currentMetadata.copy(model = newModel)
        }
    }

    fun updateInstruction(newInstruction: String) {
        updateMetadata { currentMetadata ->
            currentMetadata.copy(instruction = newInstruction)
        }
    }

    fun updateDescription(newDescription: String) {
        updateMetadata { currentMetadata ->
            currentMetadata.copy(description = newDescription)
        }
    }

    fun updateMaxTurns(newMaxTurns: Int) {
        updateMetadata { currentMetadata ->
            currentMetadata.copy(maxTurns = newMaxTurns)
        }
    }

    fun deleteAssistant(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value as? UiState.Success ?: return@launch
                assistantService.deleteAssistant(currentState.assistant.id)
                onSuccess()
            } catch (e: Exception) {
                val errorType = handleError(e)
                _uiState.value = UiState.Error(
                    message = when (errorType) {
                        ErrorType.NETWORK -> "Network error while deleting assistant. Please check your connection."
                        ErrorType.NOT_FOUND -> "Assistant not found. It may have been already deleted."
                        ErrorType.PERMISSION_DENIED -> "You don't have permission to delete this assistant."
                        ErrorType.UNKNOWN -> e.message ?: "Failed to delete assistant"
                    },
                    type = errorType
                )
            }
        }
    }

    private fun updateMetadata(
        updateBlock: (AssistantMetadataUpdate) -> AssistantMetadataUpdate
    ) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value as? UiState.Success ?: return@launch
                val currentMetadata = AssistantMetadataUpdate(
                    isPrimary = currentState.assistant.metadata?.isPrimary,
                    model = currentState.assistant.metadata?.model,
                    instruction = currentState.assistant.metadata?.instruction,
                    description = currentState.assistant.metadata?.description,
                    maxTurns = currentState.assistant.metadata?.maxTurns,
                    context = currentState.assistant.metadata?.context,
                    tools = currentState.assistant.metadata?.tools
                )
                
                val updatedMetadata = updateBlock(currentMetadata)
                val updatedAssistant = assistantService.updateAssistantMetadata(
                    currentState.assistant.id,
                    updatedMetadata
                )
                _uiState.value = UiState.Success(updatedAssistant)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to update metadata")
            }
        }
    }
}