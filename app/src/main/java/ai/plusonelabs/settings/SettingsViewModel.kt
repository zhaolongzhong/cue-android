package ai.plusonelabs.settings

import ai.plusonelabs.auth.AuthService
import ai.plusonelabs.auth.models.User
import ai.plusonelabs.ui.theme.ColorSchemeOption
import ai.plusonelabs.ui.theme.ThemeController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val version: String = "1.0.0",
    val buildNumber: String = "1",
    val theme: ColorSchemeOption = ColorSchemeOption.SYSTEM,
    val accessToken: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authService: AuthService,
    private val themeController: ThemeController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authService.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    user = user,
                )
            }
        }
        viewModelScope.launch {
            themeController.themeFlow.collect { theme ->
                _uiState.update { it.copy(theme = theme) }
            }
        }
        viewModelScope.launch {
            authService.fetchUserProfile()
        }
        loadAppVersion()
    }

    fun setTheme(theme: ColorSchemeOption) {
        viewModelScope.launch {
            themeController.setTheme(theme)
        }
    }

    fun logout() {
        authService.logout()
    }

    private fun loadAppVersion() {
        viewModelScope.launch {
            try {
                // TODO: Implement version and build number retrieval
                _uiState.update {
                    it.copy(
                        version = "1.0.0",
                        buildNumber = "1",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load app version")
                }
            }
        }
    }

    fun clearAccessToken() {
        _uiState.update { it.copy(accessToken = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
