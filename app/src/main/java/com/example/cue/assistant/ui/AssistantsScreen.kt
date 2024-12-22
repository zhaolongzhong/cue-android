package com.example.cue.assistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.assistant.model.Assistant
import com.example.cue.assistant.viewmodel.AssistantsViewModel
import com.example.cue.core.ui.components.ErrorDialog
import com.example.cue.core.ui.components.LoadingIndicator

@Composable
fun AssistantsScreen(
    viewModel: AssistantsViewModel = hiltViewModel(),
    navigateToChat: (Assistant) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var showCreateDialog by remember { mutableStateOf(false) }
    var assistantToDelete by remember { mutableStateOf<Assistant?>(null) }
    var showRenameDialog by remember { mutableStateOf<Assistant?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Assistants") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Assistant"
                        )
                    }
                    IconButton(onClick = { viewModel.onEvent(AssistantsEvent.RefreshAssistants) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )

            if (state.isLoading) {
                LoadingIndicator()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.assistants) { assistant ->
                    AssistantCard(
                        assistant = assistant,
                        isPrimary = assistant.id == state.primaryAssistant?.id,
                        onChatClick = { navigateToChat(assistant) },
                        onDeleteClick = { assistantToDelete = assistant },
                        onRenameClick = { showRenameDialog = assistant },
                        onSetPrimaryClick = {
                            viewModel.onEvent(AssistantsEvent.SetPrimaryAssistant(assistant.id))
                        }
                    )
                }
            }
        }

        // Error handling
        state.error?.let { error ->
            ErrorDialog(
                message = error.message,
                onDismiss = { viewModel.onEvent(AssistantsEvent.DismissError) }
            )
        }

        // Create Assistant Dialog
        if (showCreateDialog) {
            CreateAssistantDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    viewModel.onEvent(AssistantsEvent.CreateAssistant(name))
                    showCreateDialog = false
                }
            )
        }

        // Delete Confirmation Dialog
        assistantToDelete?.let { assistant ->
            AlertDialog(
                onDismissRequest = { assistantToDelete = null },
                title = { Text("Delete Assistant") },
                text = { Text("Are you sure you want to delete ${assistant.name}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.onEvent(AssistantsEvent.DeleteAssistant(assistant))
                            assistantToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { assistantToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Rename Dialog
        showRenameDialog?.let { assistant ->
            RenameAssistantDialog(
                currentName = assistant.name ?: "",
                onDismiss = { showRenameDialog = null },
                onRename = { newName ->
                    viewModel.onEvent(AssistantsEvent.UpdateAssistantName(assistant.id, newName))
                    showRenameDialog = null
                }
            )
        }
    }
}

@Composable
private fun AssistantCard(
    assistant: Assistant,
    isPrimary: Boolean,
    onChatClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRenameClick: () -> Unit,
    onSetPrimaryClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onChatClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assistant.name ?: "Unnamed Assistant",
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    if (isPrimary) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Primary Assistant",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = onSetPrimaryClick) {
                            Icon(
                                imageVector = Icons.Default.StarBorder,
                                contentDescription = "Set as Primary"
                            )
                        }
                    }

                    IconButton(onClick = onRenameClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename"
                        )
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete"
                        )
                    }
                }
            }

            assistant.metadata?.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateAssistantDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Assistant") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    isError = it.isBlank()
                },
                label = { Text("Assistant Name") },
                isError = isError,
                supportingText = {
                    if (isError) {
                        Text("Name cannot be empty")
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameAssistantDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Assistant") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    isError = it.isBlank()
                },
                label = { Text("Assistant Name") },
                isError = isError,
                supportingText = {
                    if (isError) {
                        Text("Name cannot be empty")
                    }
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onRename(name)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}