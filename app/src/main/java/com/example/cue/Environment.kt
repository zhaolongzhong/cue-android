package com.example.cue

import android.content.SharedPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Environment @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {
    companion object {
        private const val CLIENT_ID_KEY = "CLIENT_ID_KEY"

        // Default API base URLs for Android emulator (10.0.2.2 is localhost)
        const val DEFAULT_API_BASE_URL = "http://10.0.2.2:8000"
        const val DEFAULT_WEBSOCKET_BASE_URL = "ws://10.0.2.2:8000"
    }

    val clientId: String
        get() {
            var id = sharedPreferences.getString(CLIENT_ID_KEY, null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                sharedPreferences.edit()
                    .putString(CLIENT_ID_KEY, id)
                    .apply()
            }
            return id
        }
}
