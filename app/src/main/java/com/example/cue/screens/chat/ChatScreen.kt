package com.example.cue.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cue.ui.theme.AppTheme
import com.example.cue.viewmodels.ChatViewModel

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel = viewModel(),
    assistantId: String,
    onAssistantDetails: () -> Unit,
) {
    var showError by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(assistantId) {
        chatViewModel.setupChat(assistantId)
    }

    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.cleanup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatViewModel.assistantName) },
                actions = {
                    IconButton(onClick = onAssistantDetails) {
                        Icon(
                            imageVector = AppTheme.Icons.MoreVert,
                            contentDescription = "Assistant Details"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MessagesListView(
                messages = chatViewModel.messages,
                isLoading = chatViewModel.isLoading,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )

            MessageInputView(
                message = chatViewModel.inputMessage,
                onMessageChange = { chatViewModel.updateInputMessage(it) },
                isEnabled = chatViewModel.isInputEnabled,
                onSend = { chatViewModel.handleSendMessage() },
                focusRequester = focusRequester
            )
        }
    }

    showError?.let { error ->
        AlertDialog(
            onDismissRequest = { showError = null },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { showError = null }) {
                    Text("OK")
                }
            }
        )
    }
}