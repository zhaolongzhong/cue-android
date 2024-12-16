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

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()
    val isAuthenticated = authService.isAuthenticated

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun signUp() {
        val state = _uiState.value

        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(error = "Passwords don't match")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)

            try {
                authService.signup(
                    email = state.email.trim(),
                    password = state.password,
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
