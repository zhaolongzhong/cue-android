package com.example.cue.ui.assistant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cue.model.Assistant
import com.example.cue.ui.components.EditTextDialog
import com.example.cue.ui.components.InputDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AssistantDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showNameDialog by remember { mutableStateOf(false) }
    var showInstructionDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var showMaxTurnsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Show error message using Material Snackbar
            // TODO: Implement Snackbar handling
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Person,
                        title = "Name",
                        value = uiState.assistant.name,
                        onClick = { showNameDialog = true }
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.Tag,
                        title = "ID",
                        value = uiState.assistant.id,
                        isSelectable = true
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Settings,
                        title = "Model",
                        trailing = {
                            DropdownMenu(
                                options = uiState.availableModels,
                                selectedOption = uiState.selectedModel,
                                onOptionSelected = { viewModel.updateModel(it) }
                            )
                        }
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.ChatBubble,
                        title = "Instruction",
                        onClick = { showInstructionDialog = true },
                        showChevron = true
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.Description,
                        title = "Description",
                        onClick = { showDescriptionDialog = true },
                        showChevron = true
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.Numbers,
                        title = "Max Turns",
                        value = uiState.maxTurns.ifEmpty { "Not set" },
                        onClick = { showMaxTurnsDialog = true },
                        showChevron = true
                    )
                }
            }
        }

        if (showNameDialog) {
            InputDialog(
                title = "Update Name",
                message = "Enter a new name for this assistant",
                initialValue = uiState.assistant.name,
                onDismiss = { showNameDialog = false },
                onConfirm = { newName ->
                    viewModel.updateName(newName)
                    showNameDialog = false
                }
            )
        }

        if (showInstructionDialog) {
            EditTextDialog(
                title = "Edit Instruction",
                initialText = uiState.instruction,
                onDismiss = { showInstructionDialog = false },
                onConfirm = { newInstruction ->
                    viewModel.updateInstruction(newInstruction)
                    showInstructionDialog = false
                }
            )
        }

        if (showDescriptionDialog) {
            EditTextDialog(
                title = "Edit Description",
                initialText = uiState.description,
                onDismiss = { showDescriptionDialog = false },
                onConfirm = { newDescription ->
                    viewModel.updateDescription(newDescription)
                    showDescriptionDialog = false
                }
            )
        }

        if (showMaxTurnsDialog) {
            InputDialog(
                title = "Set Max Turns",
                message = "Enter the maximum number of conversation turns",
                initialValue = uiState.maxTurns,
                keyboardType = android.text.InputType.TYPE_CLASS_NUMBER,
                validator = { it.toIntOrNull()?.let { num -> num > 0 } ?: false },
                onDismiss = { showMaxTurnsDialog = false },
                onConfirm = { value ->
                    viewModel.updateMaxTurns(value.toIntOrNull() ?: 0)
                    showMaxTurnsDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    isSelectable: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DropdownMenu(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedOption)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}