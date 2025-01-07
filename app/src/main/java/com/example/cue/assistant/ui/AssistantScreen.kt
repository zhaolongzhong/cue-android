package com.example.cue.assistant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.models.AssistantUiState
import com.example.cue.assistant.viewmodel.AssistantViewModel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
    onAssistantClick: (Assistant) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Implement create assistant dialog */ }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Assistant")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is AssistantUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssistantUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message)
                }
            }
            is AssistantUiState.Success -> {
                AssistantList(
                    assistants = state.assistants,
                    onAssistantClick = onAssistantClick,
                    onEditClick = { /* TODO: Implement edit */ },
                    onDeleteClick = { viewModel.deleteAssistant(it.id) },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun AssistantList(
    assistants: List<Assistant>,
    onAssistantClick: (Assistant) -> Unit,
    onEditClick: (Assistant) -> Unit,
    onDeleteClick: (Assistant) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(assistants) { assistant ->
            AssistantCard(
                assistant = assistant,
                onClick = { onAssistantClick(assistant) },
                onEditClick = { onEditClick(assistant) },
                onDeleteClick = { onDeleteClick(assistant) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantCard(
    assistant: Assistant,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assistant.name,
                    style = MaterialTheme.typography.titleMedium
                )
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
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                text = "Model: ${assistant.model}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}