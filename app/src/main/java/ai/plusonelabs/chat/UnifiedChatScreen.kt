package ai.plusonelabs.chat

import ai.plusonelabs.anthropic.AnthropicChatScreen
import ai.plusonelabs.debug.DebugViewModel
import ai.plusonelabs.debug.Provider
import ai.plusonelabs.openai.OpenAIChatScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
