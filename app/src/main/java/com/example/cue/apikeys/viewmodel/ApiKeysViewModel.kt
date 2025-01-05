package com.example.cue.apikeys.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.apikeys.models.ApiKey
import com.example.cue.apikeys.service.ApiKeyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val apiKeys: List<ApiKey> = emptyList(),
        val error: Throwable? = null,
        val newKeyCreated: ApiKey? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadApiKeys()
    }

    fun refresh() {
        loadApiKeys()
    }

    fun createKey(name: String, secret: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val newKey = apiKeyService.createKey(name, secret)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        apiKeys = state.apiKeys + newKey,
                        newKeyCreated = newKey
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun updateKey(id: String, name: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val updatedKey = apiKeyService.updateKey(id, name)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        apiKeys = state.apiKeys.map { 
                            if (it.id == id) updatedKey else it 
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    fun deleteKey(apiKey: ApiKey) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                apiKeyService.deleteKey(apiKey.id)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        apiKeys = state.apiKeys.filter { it.id != apiKey.id }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }

    private fun loadApiKeys() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val keys = apiKeyService.listKeys()
                _uiState.update { it.copy(isLoading = false, apiKeys = keys) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e) }
            }
        }
    }
}