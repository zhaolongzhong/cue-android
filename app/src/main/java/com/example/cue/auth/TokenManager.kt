package com.example.cue.auth

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {
    companion object {
        private const val ACCESS_TOKEN_KEY = "ACCESS_TOKEN_KEY"
        private const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN_KEY"
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    fun saveAccessToken(token: String) {
        sharedPreferences.edit { putString(ACCESS_TOKEN_KEY, token) }
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }

    fun saveRefreshToken(token: String) {
        sharedPreferences.edit { putString(REFRESH_TOKEN_KEY, token) }
    }

    fun clearTokens() {
        sharedPreferences.edit {
            remove(ACCESS_TOKEN_KEY)
                .remove(REFRESH_TOKEN_KEY)
        }
    }

    fun hasValidAccessToken(): Boolean {
        return !getAccessToken().isNullOrEmpty()
    }
}
