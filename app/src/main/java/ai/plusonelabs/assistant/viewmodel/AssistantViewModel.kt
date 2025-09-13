package ai.plusonelabs.assistant.viewmodel

import ai.plusonelabs.assistant.AssistantRepository
import ai.plusonelabs.assistant.ClientStatusService
import ai.plusonelabs.assistant.models.Assistant
import ai.plusonelabs.assistant.models.AssistantCreationParams
import ai.plusonelabs.assistant.models.AssistantUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository,
    clientStatusService: ClientStatusService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Loading)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _selectedAssistant = MutableStateFlow<Assistant?>(null)
    val selectedAssistant: StateFlow<Assistant?> = _selectedAssistant.asStateFlow()
    val clientStatuses = clientStatusService.clientStatuses

    init {
        loadAssistants()
    }

    private fun loadAssistants() {
        viewModelScope.launch {
            _uiState.value = AssistantUiState.Loading
            repository.getAssistants()
                .onSuccess { assistants ->
                    _uiState.value = AssistantUiState.Success(assistants)
                }
                .onFailure { error ->
                    _uiState.value = AssistantUiState.Error(error.message ?: "Failed to load assistants")
                }
        }
    }

    fun selectAssistant(assistant: Assistant) {
        _selectedAssistant.value = assistant
    }

    fun createAssistant(params: AssistantCreationParams) {
        viewModelScope.launch {
            repository.createAssistant(params)
                .onSuccess { _ ->
                    loadAssistants()
                }
                .onFailure { error ->
                    _uiState.value = AssistantUiState.Error(error.message ?: "Failed to create assistant")
                }
        }
    }

    fun updateAssistant(id: String, params: AssistantCreationParams) {
        viewModelScope.launch {
            repository.updateAssistant(id, params)
                .onSuccess { _ ->
                    loadAssistants()
                }
                .onFailure { error ->
                    _uiState.value = AssistantUiState.Error(error.message ?: "Failed to update assistant")
                }
        }
    }

    fun deleteAssistant(id: String) {
        viewModelScope.launch {
            repository.deleteAssistant(id)
                .onSuccess {
                    loadAssistants()
                }
                .onFailure { error ->
                    _uiState.value = AssistantUiState.Error(error.message ?: "Failed to delete assistant")
                }
        }
    }
}
