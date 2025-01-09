package com.example.cue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {
    val isAuthenticated = authService.isAuthenticated

    init {
        viewModelScope.launch {
            if (authService.isAuthenticated.value) {
                authService.fetchUserProfile()
            }
        }
    }
}
