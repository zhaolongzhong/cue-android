package com.example.cue.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.data.UserRepository
import com.example.cue.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val version: String = "",
    val buildNumber: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        loadAppVersion()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.getCurrentUser().collect { user ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            user = user
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load user"
                    )
                }
            }
        }
    }

    private fun loadAppVersion() {
        viewModelScope.launch {
            try {
                // TODO: Implement version and build number retrieval
                _uiState.update {
                    it.copy(
                        version = "1.0.0",
                        buildNumber = "1"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load app version")
                }
            }
        }
    }

    fun generateAccessToken() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.generateAccessToken()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to generate access token"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}