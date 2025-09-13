package ai.plusonelabs.assistant.viewmodel

import ai.plusonelabs.assistant.AssistantRepository
import ai.plusonelabs.assistant.models.Assistant
import ai.plusonelabs.utils.SharedPreferencesManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawerAssistantViewModel @Inject constructor(
    assistantRepository: AssistantRepository,
    private val sharedPreferencesManager: SharedPreferencesManager,
) : ViewModel() {

    init {
        viewModelScope.launch {
            assistantRepository.refreshAssistants()
        }
    }

    val assistants: StateFlow<List<Assistant>> = assistantRepository.assistants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val selectedAssistantId: StateFlow<String?> = sharedPreferencesManager.getSelectedAssistant()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    fun setSelectedAssistant(assistantId: String?) {
        sharedPreferencesManager.setSelectedAssistant(assistantId)
    }
}
