package com.example.cue.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.auth.AuthError
import com.example.cue.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun login() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                authService.login(
                    email = _uiState.value.email.trim(),
                    password = _uiState.value.password
                )
            } catch (e: AuthError) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "An unexpected error occurred")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}