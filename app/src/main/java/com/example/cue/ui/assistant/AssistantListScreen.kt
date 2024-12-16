package com.example.cue.ui.assistant

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cue.data.model.Assistant
import com.example.cue.data.model.AssistantMetadataUpdate
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantListScreen(
    onNavigateToChat: (Assistant) -> Unit,
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistants") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Assistant")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.assistants) { assistant ->
                        AssistantItem(
                            assistant = assistant,
                            onEdit = {
                                viewModel.selectAssistant(assistant)
                                showEditDialog = true
                            },
                            onDelete = {
                                viewModel.selectAssistant(assistant)
                                showDeleteDialog = true
                            },
                            onClick = { onNavigateToChat(assistant) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAssistantDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.createAssistant(name)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog) {
        uiState.selectedAssistant?.let { assistant ->
            EditAssistantDialog(
                assistant = assistant,
                onDismiss = { showEditDialog = false },
                onConfirm = { metadata ->
                    viewModel.updateAssistant(assistant.id, metadata)
                    showEditDialog = false
                }
            )
        }
    }

    if (showDeleteDialog) {
        uiState.selectedAssistant?.let { assistant ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Assistant") },
                text = { Text("Are you sure you want to delete ${assistant.name}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAssistant(assistant.id)
                            showDeleteDialog = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantItem(
    assistant: Assistant,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = assistant.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Created: ${dateFormat.format(assistant.createdAt)}",
                style = MaterialTheme.typography.bodySmall
            )

            assistant.metadata?.let { metadata ->
                metadata.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAssistantDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Assistant") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAssistantDialog(
    assistant: Assistant,
    onDismiss: () -> Unit,
    onConfirm: (AssistantMetadataUpdate) -> Unit
) {
    var description by remember { mutableStateOf(assistant.metadata?.description.orEmpty()) }
    var instruction by remember { mutableStateOf(assistant.metadata?.instruction.orEmpty()) }
    var model by remember { mutableStateOf(assistant.metadata?.model.orEmpty()) }
    var maxTurns by remember { mutableStateOf(assistant.metadata?.maxTurns?.toString().orEmpty()) }
    var isPrimary by remember { mutableStateOf(assistant.metadata?.isPrimary ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Assistant") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Instruction") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = maxTurns,
                    onValueChange = { maxTurns = it },
                    label = { Text("Max Turns") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                    Text("Primary Assistant")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        AssistantMetadataUpdate(
                            description = description.takeIf { it.isNotBlank() },
                            instruction = instruction.takeIf { it.isNotBlank() },
                            model = model.takeIf { it.isNotBlank() },
                            maxTurns = maxTurns.toIntOrNull(),
                            isPrimary = isPrimary
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}