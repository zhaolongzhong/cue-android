package com.example.cue.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cue.anthropic.AnthropicChatScreen
import com.example.cue.debug.DebugViewModel
import com.example.cue.debug.Provider
import com.example.cue.openai.OpenAIChatScreen

@Composable
fun UnifiedChatScreen(
    modifier: Modifier = Modifier,
    debugViewModel: DebugViewModel = hiltViewModel(),
) {
    val debugUiState by debugViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        debugViewModel.loadCurrentProvider()
    }

    when (debugUiState.currentProvider) {
        Provider.OPENAI -> OpenAIChatScreen(modifier = modifier)
        Provider.ANTHROPIC -> AnthropicChatScreen(modifier = modifier)
        Provider.CUE -> AnthropicChatScreen(modifier = modifier)
    }
}
