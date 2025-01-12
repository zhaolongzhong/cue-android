package com.example.cue.assistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.assistant.AssistantRepository
import com.example.cue.assistant.models.Assistant
import com.example.cue.utils.SharedPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DrawerAssistantViewModel @Inject constructor(
    assistantRepository: AssistantRepository,
    private val sharedPreferencesManager: SharedPreferencesManager,
) : ViewModel() {

    val assistants: StateFlow<List<Assistant>> = assistantRepository.assistants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val selectedAssistantId: StateFlow<String?> = sharedPreferencesManager.getSelectedAssistant()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun setSelectedAssistant(assistantId: String?) {
        sharedPreferencesManager.setSelectedAssistant(assistantId)
    }
}