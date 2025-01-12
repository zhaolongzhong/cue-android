package com.example.cue.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesManager @Inject constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val selectedAssistantFlow = MutableStateFlow<String?>(prefs.getString(KEY_SELECTED_ASSISTANT, null))

    fun getSelectedAssistant(): StateFlow<String?> = selectedAssistantFlow

    fun setSelectedAssistant(assistantId: String?) {
        prefs.edit {
            if (assistantId == null) {
                remove(KEY_SELECTED_ASSISTANT)
            } else {
                putString(KEY_SELECTED_ASSISTANT, assistantId)
            }
        }
        selectedAssistantFlow.value = assistantId
    }

    companion object {
        private const val PREFS_NAME = "cue_preferences"
        private const val KEY_SELECTED_ASSISTANT = "selected_assistant"
    }
}
