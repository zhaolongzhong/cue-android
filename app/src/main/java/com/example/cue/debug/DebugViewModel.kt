package com.example.cue.debug

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebugUiState(
    val currentProvider: Provider = Provider.ANTHROPIC,
    val isLoading: Boolean = false,
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "DebugViewModel"
        private const val KEY_CURRENT_PROVIDER = "debug_current_provider"
    }

    fun loadCurrentProvider() {
        viewModelScope.launch {
            val savedProvider = sharedPreferences.getString(KEY_CURRENT_PROVIDER, null)
            val provider = if (savedProvider != null) {
                Provider.fromString(savedProvider) ?: Provider.ANTHROPIC
            } else {
                Provider.ANTHROPIC
            }

            Log.d(TAG, "Loaded current provider: $provider")
            _uiState.value = _uiState.value.copy(currentProvider = provider)
        }
    }

    fun selectProvider(provider: Provider) {
        viewModelScope.launch {
            Log.d(TAG, "Selecting provider: $provider")

            _uiState.value = _uiState.value.copy(
                currentProvider = provider,
                isLoading = true,
            )

            sharedPreferences.edit {
                putString(KEY_CURRENT_PROVIDER, provider.name)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)

            Log.d(TAG, "Provider selected and saved: $provider")
        }
    }

    fun getCurrentProvider(): Provider = _uiState.value.currentProvider
}
