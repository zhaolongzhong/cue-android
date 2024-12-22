package com.example.cue.settings.apikeys

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ApiKeysViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) : ViewModel() {
    var openAiKey by mutableStateOf("")
        private set
    var anthropicKey by mutableStateOf("")
        private set
    var geminiKey by mutableStateOf("")
        private set

    var editingKeyType by mutableStateOf<ApiKeyType?>(null)
        private set
    var isEditDialogVisible by mutableStateOf(false)
        private set
    var tempApiKey by mutableStateOf("")
        private set

    init {
        loadApiKeys()
    }

    private fun loadApiKeys() {
        openAiKey = sharedPreferences.getString(ApiKeyType.OPENAI.key, "") ?: ""
        anthropicKey = sharedPreferences.getString(ApiKeyType.ANTHROPIC.key, "") ?: ""
        geminiKey = sharedPreferences.getString(ApiKeyType.GEMINI.key, "") ?: ""
    }

    private fun saveKey(keyType: ApiKeyType, value: String) {
        sharedPreferences.edit().apply {
            if (value.isEmpty()) {
                remove(keyType.key)
            } else {
                putString(keyType.key, value)
            }
            apply()
        }
    }

    fun getApiKey(keyType: ApiKeyType): String = when (keyType) {
        ApiKeyType.OPENAI -> openAiKey
        ApiKeyType.ANTHROPIC -> anthropicKey
        ApiKeyType.GEMINI -> geminiKey
    }

    private fun updateApiKey(keyType: ApiKeyType, value: String) {
        val trimmedValue = value.trim()
        when (keyType) {
            ApiKeyType.OPENAI -> {
                openAiKey = trimmedValue
                saveKey(ApiKeyType.OPENAI, trimmedValue)
            }
            ApiKeyType.ANTHROPIC -> {
                anthropicKey = trimmedValue
                saveKey(ApiKeyType.ANTHROPIC, trimmedValue)
            }
            ApiKeyType.GEMINI -> {
                geminiKey = trimmedValue
                saveKey(ApiKeyType.GEMINI, trimmedValue)
            }
        }
    }

    fun startEditing(keyType: ApiKeyType) {
        editingKeyType = keyType
        tempApiKey = getApiKey(keyType)
        isEditDialogVisible = true
    }

    fun saveKey() {
        editingKeyType?.let { keyType ->
            updateApiKey(keyType, tempApiKey)
            stopEditing()
        }
    }

    fun cancelEditing() {
        tempApiKey = ""
        stopEditing()
    }

    private fun stopEditing() {
        editingKeyType = null
        isEditDialogVisible = false
    }

    fun deleteKey(keyType: ApiKeyType) {
        updateApiKey(keyType, "")
    }
}