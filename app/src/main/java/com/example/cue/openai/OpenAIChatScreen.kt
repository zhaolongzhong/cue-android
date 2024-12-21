package com.example.cue.openai

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OpenAIChatScreen(
    modifier: Modifier = Modifier,
    viewModel: OpenAIChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Effect to scroll to bottom when new message is added
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Effect to show error toast
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages) { message ->
                ChatMessage(message = message)
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = viewModel::updateInputText,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 3,
                enabled = !uiState.isLoading
            )

            IconButton(
                onClick = viewModel::sendMessage,
                enabled = !uiState.isLoading && uiState.inputText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

@Composable
private fun ChatMessage(
    message: Message,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val alignment = if (message.isUser) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) {
        dateFormat.format(Date(message.timestamp))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}