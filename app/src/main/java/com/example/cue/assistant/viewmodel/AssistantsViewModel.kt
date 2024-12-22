package com.example.cue.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.AssistantService
import com.example.cue.assistant.model.Assistant
import com.example.cue.assistant.model.AssistantError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantsViewModel @Inject constructor(
    private val assistantService: AssistantService
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantsState())
    val state: StateFlow<AssistantsState> = _state

    init {
        loadAssistants()
    }

    fun onEvent(event: AssistantsEvent) {
        when (event) {
            is AssistantsEvent.RefreshAssistants -> loadAssistants()
            is AssistantsEvent.CreateAssistant -> createAssistant(event.name)
            is AssistantsEvent.DeleteAssistant -> deleteAssistant(event.assistant)
            is AssistantsEvent.UpdateAssistantName -> updateAssistantName(event.id, event.newName)
            is AssistantsEvent.SetPrimaryAssistant -> setPrimaryAssistant(event.id)
            AssistantsEvent.DismissError -> dismissError()
        }
    }

    private fun loadAssistants() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val assistants = assistantService.getAssistants()
                val primaryAssistant = assistantService.getPrimaryAssistant()
                _state.update { it.copy(
                    assistants = assistants,
                    primaryAssistant = primaryAssistant,
                    isLoading = false,
                    error = null
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = AssistantError.LoadError(e.message ?: "Failed to load assistants")
                )}
            }
        }
    }

    private fun createAssistant(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                assistantService.createAssistant(name)
                loadAssistants()
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = AssistantError.CreateError(e.message ?: "Failed to create assistant")
                )}
            }
        }
    }

    private fun deleteAssistant(assistant: Assistant) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                assistantService.deleteAssistant(assistant.id)
                loadAssistants()
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = AssistantError.DeleteError(e.message ?: "Failed to delete assistant")
                )}
            }
        }
    }

    private fun updateAssistantName(id: String, newName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                assistantService.updateAssistantName(id, newName)
                loadAssistants()
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = AssistantError.UpdateError(e.message ?: "Failed to update assistant name")
                )}
            }
        }
    }

    private fun setPrimaryAssistant(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                assistantService.setPrimaryAssistant(id)
                loadAssistants()
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = AssistantError.UpdateError(e.message ?: "Failed to set primary assistant")
                )}
            }
        }
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}

data class AssistantsState(
    val assistants: List<Assistant> = emptyList(),
    val primaryAssistant: Assistant? = null,
    val isLoading: Boolean = false,
    val error: AssistantError? = null
)

sealed class AssistantsEvent {
    object RefreshAssistants : AssistantsEvent()
    data class CreateAssistant(val name: String) : AssistantsEvent()
    data class DeleteAssistant(val assistant: Assistant) : AssistantsEvent()
    data class UpdateAssistantName(val id: String, val newName: String) : AssistantsEvent()
    data class SetPrimaryAssistant(val id: String) : AssistantsEvent()
    object DismissError : AssistantsEvent()
}