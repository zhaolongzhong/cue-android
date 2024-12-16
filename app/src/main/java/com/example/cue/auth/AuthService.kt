package com.example.cue.auth

import android.content.SharedPreferences
import android.util.Log
import com.example.cue.auth.models.TokenResponse
import com.example.cue.auth.models.User
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AuthService"
private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"

@Singleton
class AuthService @Inject constructor(
    private val networkClient: NetworkClient,
    private val sharedPreferences: SharedPreferences
) {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isGeneratingToken = MutableStateFlow(false)
    val isGeneratingToken: StateFlow<Boolean> = _isGeneratingToken.asStateFlow()

    init {
        val token = getAccessToken()
        _isAuthenticated.value = !token.isNullOrEmpty()
    }

    suspend fun login(email: String, password: String): String {
        return try {
            val response = networkClient.post<TokenResponse>(
                "/auth/login",
                mapOf(
                    "email" to email,
                    "password" to password
                )
            )
            
            saveAccessToken(response.accessToken)
            _isAuthenticated.value = true
            response.accessToken
        } catch (e: NetworkError) {
            when (e) {
                is NetworkError.Unauthorized -> throw AuthError.InvalidCredentials
                is NetworkError.HttpError -> {
                    if (e.code == 409) throw AuthError.EmailAlreadyExists
                    throw AuthError.NetworkError
                }
                else -> {
                    Log.e(TAG, "Login error", e)
                    throw AuthError.NetworkError
                }
            }
        }
    }

    suspend fun signup(email: String, password: String, inviteCode: String? = null) {
        try {
            networkClient.post<User>(
                "/auth/signup",
                mapOf(
                    "email" to email,
                    "password" to password,
                    "invite_code" to inviteCode
                ).filterValues { it != null }
            )
            login(email, password)
        } catch (e: NetworkError) {
            when (e) {
                is NetworkError.HttpError -> {
                    if (e.code == 409) throw AuthError.EmailAlreadyExists
                    throw AuthError.NetworkError
                }
                else -> {
                    Log.e(TAG, "Signup error", e)
                    throw AuthError.NetworkError
                }
            }
        }
    }

    suspend fun generateToken(): String {
        _isGeneratingToken.value = true
        return try {
            val response = networkClient.post<TokenResponse>("/assistant/token", emptyMap())
            response.accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Token generation error", e)
            throw AuthError.TokenGenerationFailed
        } finally {
            _isGeneratingToken.value = false
        }
    }

    fun logout() {
        Log.d(TAG, "AuthService logout")
        _isAuthenticated.value = false
        removeAccessToken()
        _currentUser.value = null
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    private fun saveAccessToken(token: String) {
        sharedPreferences.edit().putString(ACCESS_TOKEN_KEY, token).apply()
    }

    private fun removeAccessToken() {
        sharedPreferences.edit().remove(ACCESS_TOKEN_KEY).apply()
    }

    suspend fun fetchUserProfile(): User? {
        return try {
            val user = networkClient.get<User>("/auth/me")
            Log.d(TAG, "fetchUserProfile userid: ${user.email}")
            _currentUser.value = user
            user
        } catch (e: Exception) {
            Log.e(TAG, "Fetch user profile error", e)
            null
        }
    }
}