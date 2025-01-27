package com.example.cue.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.models.AssistantUiState
import com.example.cue.assistant.models.ClientStatus
import com.example.cue.assistant.viewmodel.AssistantViewModel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
    onAssistantClick: (Assistant) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val clientStatuses by viewModel.clientStatuses.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Implement create assistant dialog */ },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Assistant")
            }
        },
    ) { padding ->
        when (val state = uiState) {
            is AssistantUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssistantUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.message)
                }
            }
            is AssistantUiState.Success -> {
                AssistantList(
                    assistants = state.assistants,
                    clientStatuses = clientStatuses,
                    onAssistantClick = onAssistantClick,
                    onEditClick = { /* TODO: Implement edit */ },
                    onDeleteClick = { viewModel.deleteAssistant(it.id) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun AssistantList(
    assistants: List<Assistant>,
    clientStatuses: Map<String, ClientStatus>,
    onAssistantClick: (Assistant) -> Unit,
    onEditClick: (Assistant) -> Unit,
    onDeleteClick: (Assistant) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(assistants) { assistant ->
            AssistantCard(
                assistant = assistant,
                clientStatuses = clientStatuses,
                onClick = { onAssistantClick(assistant) },
                onEditClick = { onEditClick(assistant) },
                onDeleteClick = { onDeleteClick(assistant) },
            )
        }
    }
}

@Composable
private fun AssistantCard(
    assistant: Assistant,
    clientStatuses: Map<String, ClientStatus>,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = assistant.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    clientStatuses.values.find { it.assistantId == assistant.id }?.let { status ->
                        if (status.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color.Green,
                                        shape = CircleShape,
                                    ),
                            )
                        }
                    }
                }
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            assistant.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                text = "Model: ${assistant.model}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
