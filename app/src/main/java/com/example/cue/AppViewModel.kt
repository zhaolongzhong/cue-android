package com.example.cue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.ClientStatusService
import com.example.cue.auth.AuthService
import com.example.cue.network.websocket.WebSocketService
import com.example.cue.utils.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                    AppLog.debug("Auth state changed - authenticated: $isAuthenticated, foreground: $isAppInForeground")
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
        AppLog.info("AppViewModel onAppForeground() - App came to foreground")
        isAppInForeground = true
        if (authService.isAuthenticated.value) {
            AppLog.debug("User authenticated, fetching profile and connecting WebSocket")
            viewModelScope.launch {
                authService.fetchUserProfile()
            }
            connectWebSocket()
        }
    }

    fun onAppBackground() {
        AppLog.info("AppViewModel onAppBackground() - App went to background")
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
