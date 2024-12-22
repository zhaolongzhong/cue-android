package com.cueaiclient.assistant.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cueaiclient.assistant.domain.AssistantService
import com.cueaiclient.assistant.domain.model.Assistant
import com.cueaiclient.assistant.domain.model.AssistantError
import com.cueaiclient.assistant.domain.model.AssistantMetadataUpdate
import com.cueaiclient.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AssistantsViewModel"

@HiltViewModel
class AssistantsViewModel @Inject constructor(
    private val assistantService: AssistantService
) : ViewModel() {

    private val _state = MutableStateFlow(AssistantsState())
    val state: StateFlow<AssistantsState> = _state.asStateFlow()

    init {
        fetchAssistants()
    }

    fun onEvent(event: AssistantsEvent) {
        when (event) {
            is AssistantsEvent.CreateAssistant -> createAssistant(event.name)
            is AssistantsEvent.DeleteAssistant -> deleteAssistant(event.assistant)
            is AssistantsEvent.UpdateAssistantName -> updateAssistantName(event.id, event.name)
            is AssistantsEvent.SetPrimaryAssistant -> setPrimaryAssistant(event.id)
            is AssistantsEvent.RefreshAssistants -> fetchAssistants()
            is AssistantsEvent.DismissError -> dismissError()
        }
    }

    private fun fetchAssistants(tag: String = "") {
        Log.d(TAG, "fetchAssistants for: $tag")
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = assistantService.listAssistants()) {
                is Result.Success -> {
                    _state.update { 
                        it.copy(
                            assistants = result.data,
                            primaryAssistant = result.data.find { assistant -> 
                                assistant.metadata?.isPrimary == true 
                            },
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = result.error
                        )
                    }
                    Log.e(TAG, "Error fetching assistants: ${result.error.message}")
                }
            }
        }
    }

    private fun createAssistant(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            when (val result = assistantService.createAssistant(name)) {
                is Result.Success -> {
                    _state.update { currentState ->
                        currentState.copy(
                            assistants = currentState.assistants + result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                    Log.d(TAG, "Created assistant with ID: ${result.data.id}")
                }
                is Result.Error -> {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = result.error
                        )
                    }
                    Log.e(TAG, "Error creating assistant: ${result.error.message}")
                }
            }
        }
    }

    private fun deleteAssistant(assistant: Assistant) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting assistant with ID: ${assistant.id}")
            
            when (val result = assistantService.deleteAssistant(assistant.id)) {
                is Result.Success -> {
                    _state.update { currentState ->
                        currentState.copy(
                            assistants = currentState.assistants.filterNot { it.id == assistant.id },
                            error = null
                        )
                    }
                    Log.d(TAG, "Deleted assistant with ID: ${assistant.id}")
                }
                is Result.Error -> {
                    _state.update { it.copy(error = result.error) }
                    Log.e(TAG, "Error deleting assistant: ${result.error.message}")
                }
            }
        }
    }

    private fun updateAssistantName(id: String, name: String) {
        viewModelScope.launch {
            Log.d(TAG, "Updating name for assistant ID: $id to: $name")
            
            when (val result = assistantService.updateAssistant(id = id, name = name)) {
                is Result.Success -> {
                    _state.update { currentState ->
                        val updatedAssistants = currentState.assistants.map { 
                            if (it.id == id) result.data else it 
                        }
                        currentState.copy(
                            assistants = updatedAssistants,
                            error = null
                        )
                    }
                    Log.d(TAG, "Updated assistant name for ID: $id")
                }
                is Result.Error -> {
                    _state.update { it.copy(error = result.error) }
                    Log.e(TAG, "Error updating assistant name: ${result.error.message}")
                }
            }
        }
    }

    private fun setPrimaryAssistant(id: String) {
        viewModelScope.launch {
            Log.d(TAG, "Setting primary assistant to ID: $id")
            
            val previousPrimaryId = state.value.primaryAssistant?.id
            
            when (val result = assistantService.updateAssistant(
                id = id,
                metadata = AssistantMetadataUpdate(isPrimary = true)
            )) {
                is Result.Success -> {
                    _state.update { currentState ->
                        val updatedAssistants = currentState.assistants.toMutableList()
                        val index = updatedAssistants.indexOfFirst { it.id == id }
                        if (index != -1) {
                            updatedAssistants[index] = result.data
                        }
                        
                        currentState.copy(
                            assistants = updatedAssistants,
                            primaryAssistant = result.data,
                            error = null
                        )
                    }
                    
                    if (previousPrimaryId != null && previousPrimaryId != id) {
                        when (val prevResult = assistantService.getAssistant(previousPrimaryId)) {
                            is Result.Success -> {
                                _state.update { currentState ->
                                    val updatedAssistants = currentState.assistants.map { 
                                        if (it.id == previousPrimaryId) prevResult.data else it 
                                    }
                                    currentState.copy(assistants = updatedAssistants)
                                }
                            }
                            is Result.Error -> Log.e(TAG, "Error updating previous primary assistant: ${prevResult.error.message}")
                        }
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(error = result.error) }
                    Log.e(TAG, "Error setting primary assistant: ${result.error.message}")
                }
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
    data class CreateAssistant(val name: String) : AssistantsEvent()
    data class DeleteAssistant(val assistant: Assistant) : AssistantsEvent()
    data class UpdateAssistantName(val id: String, val name: String) : AssistantsEvent()
    data class SetPrimaryAssistant(val id: String) : AssistantsEvent()
    data object RefreshAssistants : AssistantsEvent()
    data object DismissError : AssistantsEvent()
}