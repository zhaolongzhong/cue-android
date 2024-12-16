package com.example.cue.ui.tools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.cue.tools.LocalTool
import com.example.cue.tools.ToolManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ToolsListViewModel @Inject constructor(
    toolManager: ToolManager
) : ViewModel() {
    var tools by mutableStateOf(toolManager.getAvailableTools())
        private set
}