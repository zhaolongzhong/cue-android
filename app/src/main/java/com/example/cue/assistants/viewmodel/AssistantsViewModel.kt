package com.example.cue.assistants.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistants.models.Assistant
import com.example.cue.assistants.models.AssistantMetadataUpdate
import com.example.cue.assistants.service.AssistantError
import com.example.cue.assistants.service.AssistantService
import com.example.cue.network.ClientStatus
import com.example.cue.websocket.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AssistantsViewModel @Inject constructor(
    private val assistantService: AssistantService,
    private val webSocketService: WebSocketService
) : ViewModel() {

    data class UiState(
        val assistants: List<Assistant> = emptyList(),
        val clientStatuses: Map<String, ClientStatus> = emptyMap(),
        val isLoading: Boolean = false,
        val error: AssistantError? = null,
        val primaryAssistant: Assistant? = null,
        val assistantToDelete: Assistant? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        setupClientStatusSubscriptions()
        setupPrimaryAssistantTracking()
    }

    private fun setupClientStatusSubscriptions() {
        viewModelScope.launch {
            webSocketService.clientStatuses.collect { statuses ->
                val statusDict = statuses.values
                    .mapNotNull { status ->
                        status.assistantId?.let { id ->
                            id to status
                        }
                    }
                    .toMap()

                _uiState.update { state ->
                    state.copy(clientStatuses = statusDict)
                }

                findUnmatchedAssistants()
            }
        }
    }

    private fun setupPrimaryAssistantTracking() {
        viewModelScope.launch {
            _uiState.collect { state ->
                val primary = state.assistants.find { it.metadata?.isPrimary == true }
                if (primary != _uiState.value.primaryAssistant) {
                    _uiState.update { it.copy(primaryAssistant = primary) }
                }
            }
        }
    }

    fun fetchAssistants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            assistantService.listAssistants()
                .onSuccess { assistants ->
                    _uiState.update { it.copy(assistants = assistants) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error as? AssistantError ?: AssistantError.Unknown)
                    }
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteAssistant(assistant: Assistant) {
        viewModelScope.launch {
            assistantService.deleteAssistant(assistant.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            assistants = state.assistants.filter { it.id != assistant.id },
                            assistantToDelete = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error as? AssistantError ?: AssistantError.Unknown)
                    }
                }
        }
    }

    fun createAssistant(name: String? = null, isPrimary: Boolean = false) {
        viewModelScope.launch {
            assistantService.createAssistant(name, isPrimary)
                .onSuccess { assistant ->
                    _uiState.update { state ->
                        state.copy(assistants = state.assistants + assistant)
                    }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error as? AssistantError ?: AssistantError.Unknown)
                    }
                }
        }
    }

    fun updateAssistantName(id: String, name: String) {
        viewModelScope.launch {
            assistantService.updateAssistant(id = id, name = name)
                .onSuccess { assistant ->
                    updateAssistantInList(assistant)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error as? AssistantError ?: AssistantError.Unknown)
                    }
                }
        }
    }

    fun updateMetadata(
        id: String,
        isPrimary: Boolean? = null,
        model: String? = null,
        instruction: String? = null,
        description: String? = null,
        maxTurns: Int? = null,
        context: String? = null,
        tools: List<String>? = null
    ) {
        viewModelScope.launch {
            val metadata = AssistantMetadataUpdate(
                isPrimary = isPrimary,
                model = model,
                instruction = instruction,
                description = description,
                maxTurns = maxTurns,
                context = context,
                tools = tools
            )

            assistantService.updateAssistant(id = id, metadata = metadata)
                .onSuccess { assistant ->
                    updateAssistantInList(assistant)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(error = error as? AssistantError ?: AssistantError.Unknown)
                    }
                }
        }
    }

    fun setPrimaryAssistant(id: String) {
        viewModelScope.launch {
            val previousPrimaryId = _uiState.value.primaryAssistant?.id

            assistantService.updateAssistant(
                id = id,
                metadata = AssistantMetadataUpdate(isPrimary = true)
            ).onSuccess { assistant ->
                updateAssistantInList(assistant)

                // Update previous primary if it exists and is different
                if (previousPrimaryId != null && previousPrimaryId != id) {
                    assistantService.getAssistant(previousPrimaryId)
                        .onSuccess { previousAssistant ->
                            updateAssistantInList(previousAssistant)
                        }
                }
            }
        }
    }

    private fun updateAssistantInList(assistant: Assistant) {
        _uiState.update { state ->
            val updatedList = state.assistants.map {
                if (it.id == assistant.id) assistant else it
            }
            state.copy(assistants = updatedList)
        }
    }

    private fun findUnmatchedAssistants() {
        viewModelScope.launch {
            val existingIds = _uiState.value.assistants.map { it.id }.toSet()
            val statusIds = _uiState.value.clientStatuses.keys.toSet()
            val missingIds = statusIds - existingIds

            missingIds.forEach { id ->
                assistantService.getAssistant(id)
                    .onSuccess { assistant ->
                        _uiState.update { state ->
                            state.copy(assistants = state.assistants + assistant)
                        }
                    }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setAssistantToDelete(assistant: Assistant?) {
        _uiState.update { it.copy(assistantToDelete = assistant) }
    }
}