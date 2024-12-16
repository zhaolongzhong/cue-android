package com.example.cue.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.data.APIKeyRepository
import com.example.cue.data.UserRepository
import com.example.cue.data.WebSocketStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKey: String? = null,
    val currentUserId: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiKeyRepository: APIKeyRepository,
    private val userRepository: UserRepository,
    private val webSocketStore: WebSocketStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load API key
                apiKeyRepository.getAPIKey("openai").collect { apiKey ->
                    _uiState.update { it.copy(apiKey = apiKey) }
                }

                // Load current user
                userRepository.getCurrentUser().collect { user ->
                    _uiState.update { it.copy(currentUserId = user?.id) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load initial data")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        loadInitialData()
    }

    fun initializeWebSocket(userId: String) {
        webSocketStore.initialize(userId)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}