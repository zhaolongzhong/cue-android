package com.example.cue.assistant.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${currentState.message}")
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

    // Similar dialogs for instruction, description, and max turns...
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