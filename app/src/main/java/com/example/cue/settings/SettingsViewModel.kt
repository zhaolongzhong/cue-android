package com.example.cue.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userEmail: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    userEmail = user?.email,
                )
            }
        }
        viewModelScope.launch {
            authService.fetchUserProfile()
        }
    }

    fun logout() {
        authService.logout()
    }
}
