package com.example.cue.ui.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cue.ui.session.components.ChatInput
import com.example.cue.ui.session.components.ClientSelector
import com.example.cue.ui.session.components.MessageList
import com.example.cue.ui.session.components.ModeSelector
import com.example.cue.ui.session.components.SessionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionChatScreen(
    session: CLISession,
    modifier: Modifier = Modifier,
    viewModel: SessionChatViewModel = hiltViewModel(),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val cliMode by viewModel.cliMode.collectAsStateWithLifecycle()
    val availableClients by viewModel.availableClients.collectAsStateWithLifecycle()
    val selectedClientId by viewModel.selectedClientId.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val isDiscovering by viewModel.isDiscovering.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showModeSelector by remember { mutableStateOf(false) }
    var showClientSelector by remember { mutableStateOf(false) }

    // Initialize session and connect to WebSocket when screen appears
    DisposableEffect(session.id) {
        viewModel.initializeSession(session)
        viewModel.connectWebSocket()
        onDispose {
            viewModel.disconnectWebSocket()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Session Header
            currentSession?.let { session ->
                SessionHeader(
                    session = session,
                    cliMode = cliMode,
                    availableClients = availableClients,
                    selectedClientId = selectedClientId,
                    connectionStatus = connectionStatus,
                    onModeClick = { showModeSelector = true },
                    onClientClick = { showClientSelector = true },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Message List
            MessageList(
                messages = messages,
                cliMode = cliMode,
                isLoading = isLoading,
                listState = listState,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chat Input
            ChatInput(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSendMessage = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                isLoading = isLoading,
                cliMode = cliMode,
            )
        }

        // Mode Selector Dialog
        if (showModeSelector) {
            ModeSelector(
                currentMode = cliMode,
                onModeSelect = { mode ->
                    if (mode != cliMode) viewModel.toggleMode()
                    showModeSelector = false
                },
                onDismiss = { showModeSelector = false },
            )
        }

        // Client Selector Dialog
        if (showClientSelector) {
            ClientSelector(
                availableClients = availableClients,
                selectedClientId = selectedClientId,
                isDiscovering = isDiscovering,
                onClientSelected = { clientId ->
                    viewModel.selectClient(clientId)
                    showClientSelector = false
                },
                onRefreshClients = { viewModel.refreshClients() },
                onDismiss = { showClientSelector = false },
            )
        }
    }
}
