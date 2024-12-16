package com.example.cue.ui.assistant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.data.AssistantRepository
import com.example.cue.model.Assistant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssistantDetailUiState(
    val isLoading: Boolean = false,
    val assistant: Assistant = Assistant(id = "", name = ""),
    val error: String? = null,
    val selectedModel: String = "",
    val instruction: String = "",
    val description: String = "",
    val maxTurns: String = "",
    val availableModels: List<String> = listOf(
        "claude-3-5-sonnet-20241022",
        "gpt-4o-mini",
        "gpt-4o",
        "o1-mini"
    )
)

@HiltViewModel
class AssistantDetailViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val assistantId: String = checkNotNull(savedStateHandle["assistantId"])
    private val _uiState = MutableStateFlow(AssistantDetailUiState())
    val uiState: StateFlow<AssistantDetailUiState> = _uiState.asStateFlow()

    init {
        loadAssistant()
    }

    private fun loadAssistant() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                assistantRepository.getAssistant(assistantId).collect { assistant ->
                    assistant?.let {
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                assistant = it,
                                selectedModel = it.metadata?.model ?: state.availableModels[0],
                                instruction = it.metadata?.instruction ?: "",
                                description = it.metadata?.description ?: "",
                                maxTurns = it.metadata?.maxTurns?.toString() ?: ""
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load assistant"
                    )
                }
            }
        }
    }

    fun updateName(newName: String) {
        viewModelScope.launch {
            try {
                val updatedAssistant = assistantRepository.updateAssistantName(
                    assistantId,
                    newName
                )
                _uiState.update {
                    it.copy(assistant = updatedAssistant)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update name")
                }
            }
        }
    }

    private fun updateMetadata(
        model: String? = null,
        instruction: String? = null,
        description: String? = null,
        maxTurns: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val updatedAssistant = assistantRepository.updateMetadata(
                    assistantId,
                    model,
                    instruction,
                    description,
                    maxTurns
                )
                _uiState.update {
                    it.copy(
                        assistant = updatedAssistant,
                        instruction = updatedAssistant.metadata?.instruction ?: "",
                        description = updatedAssistant.metadata?.description ?: "",
                        maxTurns = updatedAssistant.metadata?.maxTurns?.toString() ?: ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update metadata")
                }
            }
        }
    }

    fun updateModel(model: String) {
        updateMetadata(model = model)
    }

    fun updateInstruction(instruction: String) {
        updateMetadata(instruction = instruction)
    }

    fun updateDescription(description: String) {
        updateMetadata(description = description)
    }

    fun updateMaxTurns(maxTurns: Int) {
        updateMetadata(maxTurns = maxTurns)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}