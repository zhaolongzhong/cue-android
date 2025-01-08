package com.example.cue.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.viewmodel.AssistantDetailViewModel
import com.example.cue.settings.views.SettingsRow

@Composable
fun AssistantDetailScreen(
    assistantId: String,
    onNavigateBack: () -> Unit,
    viewModel: AssistantDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(assistantId) {
        viewModel.loadAssistant(assistantId)
    }

    val state by viewModel.uiState.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var showInstructionDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showMaxTurnsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteConfirmation = true }
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete assistant",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val currentState = state) {
            is AssistantDetailViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AssistantDetailViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (currentState.type) {
                                AssistantDetailViewModel.ErrorType.NETWORK -> Icons.Outlined.CloudOff
                                AssistantDetailViewModel.ErrorType.NOT_FOUND -> Icons.Outlined.ErrorOutline
                                AssistantDetailViewModel.ErrorType.PERMISSION_DENIED -> Icons.Outlined.Lock
                                else -> Icons.Outlined.Error
                            },
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        if (currentState.type == AssistantDetailViewModel.ErrorType.NETWORK) {
                            Button(
                                onClick = { viewModel.loadAssistant(assistantId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            is AssistantDetailViewModel.UiState.Success -> {
                AssistantDetailContent(
                    assistant = currentState.assistant,
                    modifier = Modifier.padding(padding),
                    onNameClick = { showNameDialog = true },
                    onInstructionClick = { showInstructionDialog = true },
                    onDescriptionClick = { showDescriptionDialog = true },
                    onMaxTurnsClick = { showMaxTurnsDialog = true },
                    onModelSelected = { model -> viewModel.updateModel(model) }
                )
            }
        }
    }

    // Dialogs
    if (showNameDialog) {
        val assistant = (state as? AssistantDetailViewModel.UiState.Success)?.assistant
        var name by remember { mutableStateOf(assistant?.name ?: "") }
        
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Update Name") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateName(name)
                        showNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInstructionDialog) {
        val assistant = (state as? AssistantDetailViewModel.UiState.Success)?.assistant
        var instruction by remember { mutableStateOf(assistant?.metadata?.instruction ?: "") }
        
        AlertDialog(
            onDismissRequest = { showInstructionDialog = false },
            title = { Text("Update Instruction") },
            text = {
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Instruction") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateInstruction(instruction)
                        showInstructionDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstructionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDescriptionDialog) {
        val assistant = (state as? AssistantDetailViewModel.UiState.Success)?.assistant
        var description by remember { mutableStateOf(assistant?.metadata?.description ?: "") }
        
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = false },
            title = { Text("Update Description") },
            text = {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateDescription(description)
                        showDescriptionDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDescriptionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Assistant") },
            text = { Text("Are you sure you want to delete this assistant? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAssistant(onSuccess = onNavigateBack)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMaxTurnsDialog) {
        val assistant = (state as? AssistantDetailViewModel.UiState.Success)?.assistant
        var maxTurns by remember { mutableStateOf(assistant?.metadata?.maxTurns?.toString() ?: "") }
        var error by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showMaxTurnsDialog = false },
            title = { Text("Update Max Turns") },
            text = {
                Column {
                    OutlinedTextField(
                        value = maxTurns,
                        onValueChange = { 
                            maxTurns = it
                            error = when {
                                it.isEmpty() -> null
                                it.toIntOrNull() == null -> "Please enter a valid number"
                                it.toInt() <= 0 -> "Number must be greater than 0"
                                else -> null
                            }
                        },
                        label = { Text("Max Turns") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = error != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        maxTurns.toIntOrNull()?.let { value ->
                            viewModel.updateMaxTurns(value)
                            showMaxTurnsDialog = false
                        }
                    },
                    enabled = error == null && maxTurns.isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMaxTurnsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AssistantDetailContent(
    assistant: Assistant,
    modifier: Modifier = Modifier,
    onNameClick: () -> Unit,
    onInstructionClick: () -> Unit,
    onDescriptionClick: () -> Unit,
    onMaxTurnsClick: () -> Unit,
    onModelSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        item {
            SettingsRow(
                icon = Icons.Default.Person,
                title = "Name",
                subtitle = assistant.name,
                onClick = onNameClick
            )
        }

        item {
            SettingsRow(
                icon = Icons.Outlined.Tag,
                title = "ID",
                subtitle = assistant.id,
                onClick = null // Read-only
            )
        }

        item {
            ModelSelector(
                currentModel = assistant.metadata?.model ?: "",
                onModelSelected = onModelSelected
            )
        }

        item {
            SettingsRow(
                icon = Icons.Outlined.Description,
                title = "Instruction",
                subtitle = assistant.metadata?.instruction ?: "Not set",
                onClick = onInstructionClick
            )
        }

        item {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "Description",
                subtitle = assistant.metadata?.description ?: "Not set",
                onClick = onDescriptionClick
            )
        }

        item {
            SettingsRow(
                icon = Icons.Outlined.Numbers,
                title = "Max Turns",
                subtitle = assistant.metadata?.maxTurns?.toString() ?: "Not set",
                onClick = onMaxTurnsClick
            )
        }
    }
}

@Composable
private fun ModelSelector(
    currentModel: String,
    onModelSelected: (String) -> Unit
) {
    val models = remember { listOf("gpt-4", "gpt-3.5-turbo", "claude-2", "gemini-pro") }
    
    var expanded by remember { mutableStateOf(false) }

    SettingsRow(
        icon = Icons.Outlined.SmartToy,
        title = "Model",
        subtitle = currentModel.ifEmpty { "Not set" },
        onClick = { expanded = true },
        trailing = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select model")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    models.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}