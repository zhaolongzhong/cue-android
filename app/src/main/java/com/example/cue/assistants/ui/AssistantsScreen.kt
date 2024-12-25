package com.example.cue.assistants.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.assistants.models.Assistant
import com.example.cue.assistants.service.AssistantError
import com.example.cue.assistants.viewmodel.AssistantsViewModel
import com.example.cue.network.ClientStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantsScreen(
    onNavigateToChat: (Assistant) -> Unit,
    onNavigateToDetails: (Assistant) -> Unit,
    viewModel: AssistantsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var newAssistantName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchAssistants()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistants") },
                actions = {
                    IconButton(onClick = { showNameDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Assistant")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AssistantsList(
                assistants = uiState.assistants,
                clientStatuses = uiState.clientStatuses,
                onDelete = { assistant -> viewModel.setAssistantToDelete(assistant) },
                onDetails = onNavigateToDetails,
                onSetPrimary = { assistant -> viewModel.setPrimaryAssistant(assistant.id) },
                onChat = onNavigateToChat
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            // Error handling
            uiState.error?.let { error ->
                val errorMessage = when (error) {
                    is AssistantError.NetworkError -> "Network error occurred"
                    is AssistantError.InvalidResponse -> "Invalid response from server"
                    is AssistantError.NotFound -> "Assistant not found"
                    is AssistantError.Unknown -> "An unknown error occurred"
                }

                LaunchedEffect(error) {
                    // Show snackbar or dialog with error message
                    // For now, just clear the error after showing
                    viewModel.clearError()
                }
            }

            // Delete confirmation dialog
            uiState.assistantToDelete?.let { assistant ->
                AlertDialog(
                    onDismissRequest = { viewModel.setAssistantToDelete(null) },
                    title = { Text("Delete Assistant") },
                    text = { Text("Are you sure you want to delete "${assistant.name}"?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteAssistant(assistant)
                            }
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setAssistantToDelete(null) }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // New assistant name dialog
            if (showNameDialog) {
                AlertDialog(
                    onDismissRequest = { showNameDialog = false },
                    title = { Text("New Assistant") },
                    text = {
                        OutlinedTextField(
                            value = newAssistantName,
                            onValueChange = { newAssistantName = it },
                            label = { Text("Assistant Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.createAssistant(name = newAssistantName.takeIf { it.isNotBlank() })
                                showNameDialog = false
                                newAssistantName = ""
                            }
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showNameDialog = false
                                newAssistantName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AssistantsList(
    assistants: List<Assistant>,
    clientStatuses: Map<String, ClientStatus>,
    onDelete: (Assistant) -> Unit,
    onDetails: (Assistant) -> Unit,
    onSetPrimary: (Assistant) -> Unit,
    onChat: (Assistant) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(assistants) { assistant ->
            AssistantRow(
                assistant = assistant,
                clientStatus = clientStatuses[assistant.id],
                onDelete = { onDelete(assistant) },
                onDetails = { onDetails(assistant) },
                onSetPrimary = { onSetPrimary(assistant) },
                onChat = { onChat(assistant) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantRow(
    assistant: Assistant,
    clientStatus: ClientStatus?,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onSetPrimary: () -> Unit,
    onChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onDetails
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = assistant.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (assistant.metadata?.isPrimary == true) {
                    Text(
                        text = "Primary",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onChat) {
                    Text("Chat")
                }
                if (assistant.metadata?.isPrimary != true) {
                    TextButton(onClick = onSetPrimary) {
                        Text("Set Primary")
                    }
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }

            clientStatus?.let { status ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = status.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}