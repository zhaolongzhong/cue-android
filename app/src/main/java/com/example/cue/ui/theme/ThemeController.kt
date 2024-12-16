package com.example.cue.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class ThemeController @Inject constructor(
    private val context: Context,
) {
    val themeFlow: Flow<ColorSchemeOption> = context.dataStore.data.map { preferences ->
        try {
            ColorSchemeOption.valueOf(
                preferences[THEME_KEY] ?: ColorSchemeOption.SYSTEM.name,
            )
        } catch (e: IllegalArgumentException) {
            ColorSchemeOption.SYSTEM
        }
    }

    suspend fun setTheme(theme: ColorSchemeOption) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_setting")
    }
}
