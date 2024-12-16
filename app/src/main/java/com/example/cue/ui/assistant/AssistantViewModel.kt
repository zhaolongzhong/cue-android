package com.example.cue.ui.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.data.model.Assistant
import com.example.cue.data.model.AssistantCreate
import com.example.cue.data.model.AssistantMetadataUpdate
import com.example.cue.data.repository.AssistantRepository
import com.example.cue.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantUiState(
    val assistants: List<Assistant> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAssistant: Assistant? = null
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val repository: AssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    init {
        loadAssistants()
    }

    fun loadAssistants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.listAssistants()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            assistants = result.data,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun selectAssistant(assistant: Assistant) {
        _uiState.update { it.copy(selectedAssistant = assistant) }
    }

    fun createAssistant(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.createAssistant(AssistantCreate(name))) {
                is NetworkResult.Success -> {
                    loadAssistants()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun updateAssistant(id: String, metadata: AssistantMetadataUpdate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.updateAssistant(id, metadata)) {
                is NetworkResult.Success -> {
                    loadAssistants()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun deleteAssistant(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.deleteAssistant(id)) {
                is NetworkResult.Success -> {
                    loadAssistants()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}