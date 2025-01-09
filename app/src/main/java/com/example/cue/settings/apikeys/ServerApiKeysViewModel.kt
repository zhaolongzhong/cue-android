package com.example.cue.settings.apikeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.auth.AuthError
import com.example.cue.settings.apikeys.models.ApiKey
import com.example.cue.settings.apikeys.models.ApiKeyPrivate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ServerApiKeysViewModel @Inject constructor(
    private val apiKeyService: ApiKeyService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerApiKeysUiState())
    val uiState: StateFlow<ServerApiKeysUiState> = _uiState.asStateFlow()

    private val pageSize = 20
    private var currentPage = 0
    private var hasMorePages = true

    init {
        fetch()
    }

    fun fetch() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            apiKeyService.listApiKeys(
                skip = currentPage * pageSize,
                limit = pageSize
            ).onSuccess { keys ->
                _uiState.update { state ->
                    state.copy(
                        apiKeys = if (currentPage == 0) keys else state.apiKeys + keys,
                        isLoading = false
                    )
                }
                hasMorePages = keys.size == pageSize
                currentPage++
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error as? AuthError ?: AuthError.Unknown
                )}
            }
        }
    }

    fun createNewApiKey(name: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val effectiveName = name.ifEmpty { "My API Key" }
            apiKeyService.createApiKey(
                name = effectiveName,
                keyType = _uiState.value.selectedKeyType,
                scopes = _uiState.value.selectedScopes.takeIf { it.isNotEmpty() },
                expiresAt = _uiState.value.expirationDate
            ).onSuccess { privateKey ->
                _uiState.update { state ->
                    state.copy(
                        apiKeys = listOf(privateKey.toPublicKey()) + state.apiKeys,
                        newKeyCreated = privateKey,
                        isLoading = false
                    )
                }
                resetForm()
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error as? AuthError ?: AuthError.Unknown
                )}
            }
        }
    }

    fun updateKey(key: ApiKey, name: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val effectiveName = name.ifEmpty { key.name }
            apiKeyService.updateApiKey(
                id = key.id,
                name = effectiveName,
                scopes = null,  // Keep existing scopes
                expiresAt = null,  // Keep existing expiration
                isActive = null  // Keep existing active state
            ).onSuccess { updatedKey ->
                _uiState.update { state ->
                    val updatedKeys = state.apiKeys.map { 
                        if (it.id == key.id) updatedKey else it 
                    }
                    state.copy(apiKeys = updatedKeys, isLoading = false)
                }
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error as? AuthError ?: AuthError.Unknown
                )}
            }
        }
    }

    fun deleteKey(key: ApiKey) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            apiKeyService.deleteApiKey(key.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            apiKeys = state.apiKeys.filter { it.id != key.id },
                            isLoading = false
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error as? AuthError ?: AuthError.Unknown
                    )}
                }
        }
    }

    fun refresh() {
        currentPage = 0
        hasMorePages = true
        fetch()
    }

    fun loadMoreIfNeeded(currentItem: ApiKey) {
        if (!_uiState.value.isLoading && 
            hasMorePages && 
            _uiState.value.apiKeys.lastOrNull()?.id == currentItem.id
        ) {
            fetch()
        }
    }

    private fun resetForm() {
        _uiState.update { it.copy(
            selectedKeyType = "live",
            selectedScopes = listOf("all"),
            expirationDate = null
        )}
    }

    fun updateSelectedKeyType(keyType: String) {
        _uiState.update { it.copy(selectedKeyType = keyType) }
    }

    fun updateSelectedScopes(scopes: List<String>) {
        _uiState.update { it.copy(selectedScopes = scopes) }
    }

    fun updateExpirationDate(date: Date?) {
        _uiState.update { it.copy(expirationDate = date) }
    }

    fun clearNewKeyCreated() {
        _uiState.update { it.copy(newKeyCreated = null) }
    }

    fun toggleAddKeyDialog(show: Boolean) {
        _uiState.update { it.copy(isShowingAddKey = show) }
    }
}

data class ServerApiKeysUiState(
    val apiKeys: List<ApiKey> = emptyList(),
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val selectedKeyType: String = "live",
    val selectedScopes: List<String> = listOf("all"),
    val expirationDate: Date? = null,
    val isShowingAddKey: Boolean = false,
    val newKeyCreated: ApiKeyPrivate? = null
)