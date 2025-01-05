package com.example.cue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.ClientStatusService
import com.example.cue.auth.AuthService
import com.example.cue.network.websocket.WebSocketService
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
                    Log.d(
                        TAG,
                        "Auth state changed - authenticated: $isAuthenticated, foreground: $isAppInForeground",
                    )
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
        isAppInForeground = true
        if (authService.isAuthenticated.value) {
            viewModelScope.launch {
                authService.fetchUserProfile()
            }
            connectWebSocket()
        }
    }

    fun onAppBackground() {
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
