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
