package com.example.cue.auth

import android.content.SharedPreferences
import com.example.cue.auth.models.TokenResponse
import com.example.cue.auth.models.User
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import com.example.cue.utils.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthService"

@Singleton
class AuthService @Inject constructor(
    private val networkClient: NetworkClient,
    private val sharedPreferences: SharedPreferences,
    private val tokenManager: TokenManager,
) {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isGeneratingToken = MutableStateFlow(false)
    val isGeneratingToken: StateFlow<Boolean> = _isGeneratingToken.asStateFlow()

    init {
        _isAuthenticated.value = tokenManager.hasValidAccessToken()
    }

    suspend fun login(email: String, password: String): String {
        AppLog.info("AuthService.login() - Attempting login for user: $email")
        return try {
            val response = networkClient.postFormUrlEncoded<TokenResponse>(
                "/accounts/login",
                mapOf(
                    "grant_type" to "password",
                    "username" to email,
                    "password" to password,
                ),
                TokenResponse::class.java,
            )

            AppLog.info("AuthService.login() - Login successful, saving token")
            tokenManager.saveAccessToken(response.accessToken)
            _isAuthenticated.value = true
            fetchUserProfile()
            response.accessToken
        } catch (e: NetworkError) {
            when (e) {
                is NetworkError.Unauthorized -> throw AuthError.InvalidCredentials
                is NetworkError.HttpError -> {
                    if (e.code == 409) throw AuthError.EmailAlreadyExists
                    throw AuthError.NetworkError
                }
                else -> {
                    AppLog.error("Login error", e)
                    throw AuthError.NetworkError
                }
            }
        }
    }

    suspend fun signup(email: String, password: String, inviteCode: String? = null) {
        try {
            // Make API call to create user
            networkClient.post<User>(
                "/accounts/signup",
                mapOf(
                    "email" to email,
                    "password" to password,
                    "invite_code" to inviteCode,
                ).filterValues { it != null },
                User::class.java,
            )
            login(email, password)
        } catch (e: NetworkError) {
            when (e) {
                is NetworkError.HttpError -> {
                    if (e.code == 409) throw AuthError.EmailAlreadyExists
                    throw AuthError.NetworkError
                }
                else -> {
                    AppLog.error("Signup error", e)
                    throw AuthError.NetworkError
                }
            }
        }
    }

    suspend fun fetchUserProfile(): User? {
        return try {
            val user = networkClient.get<User>(
                "/accounts/me",
                User::class.java,
            )
            AppLog.debug("fetchUserProfile userid: ${user.email}")
            _currentUser.value = user
            user
        } catch (e: Exception) {
            AppLog.error("Fetch user profile error", e)
            null
        }
    }

    fun logout() {
        AppLog.debug("AuthService logout")
        _isAuthenticated.value = false
        tokenManager.clearTokens()
        _currentUser.value = null
    }
}
