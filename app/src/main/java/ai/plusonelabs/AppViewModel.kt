package ai.plusonelabs

import ai.plusonelabs.assistant.ClientStatusService
import ai.plusonelabs.auth.AuthService
import ai.plusonelabs.network.websocket.WebSocketService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import ai.plusonelabs.utils.AppLog as Log

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authService: AuthService,
    private val webSocketService: WebSocketService,
    private val clientStatusService: ClientStatusService,
) : ViewModel() {

    companion object {
        private const val TAG = "AppViewModel"
    }

    val isAuthenticated = authService.isAuthenticated
    val clientStatuses = clientStatusService.clientStatuses
    private var webSocketJob: Job? = null
    private var isAppInForeground = false

    init {
        viewModelScope.launch {
            authService.isAuthenticated
                .onEach { isAuthenticated ->
                    Log.d("AppViewModel", "Auth state changed - authenticated: $isAuthenticated, foreground: $isAppInForeground")
                    if (isAuthenticated && isAppInForeground) {
                        authService.fetchUserProfile()
                        connectWebSocket()
                    } else {
                        disconnectWebSocket()
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(),
                    initialValue = authService.isAuthenticated.value,
                )
                .collect()
        }
    }

    fun onAppForeground() {
        Log.i("AppViewModel", "onAppForeground() - App came to foreground")
        isAppInForeground = true
        if (authService.isAuthenticated.value) {
            Log.d("AppViewModel", "User authenticated, fetching profile and connecting WebSocket")
            viewModelScope.launch {
                authService.fetchUserProfile()
            }
            connectWebSocket()
        }
    }

    fun onAppBackground() {
        Log.i("AppViewModel", "onAppBackground() - App went to background")
        isAppInForeground = false
        disconnectWebSocket()
    }

    private fun connectWebSocket() {
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            webSocketService.connect()
        }
    }

    private fun disconnectWebSocket() {
        webSocketJob?.cancel()
        webSocketJob = null
        viewModelScope.launch {
            webSocketService.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}
