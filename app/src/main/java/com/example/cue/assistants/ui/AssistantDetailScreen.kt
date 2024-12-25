package com.example.cue.assistants.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cue.assistants.models.Assistant
import com.example.cue.assistants.viewmodel.AssistantsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantDetailScreen(
    assistant: Assistant,
    viewModel: AssistantsViewModel,
    onNavigateBack: () -> Unit
) {
    var editName by remember { mutableStateOf(assistant.name) }
    var editModel by remember { mutableStateOf(assistant.metadata?.model ?: "") }
    var editInstruction by remember { mutableStateOf(assistant.metadata?.instruction ?: "") }
    var editDescription by remember { mutableStateOf(assistant.metadata?.description ?: "") }
    var editMaxTurns by remember { mutableStateOf(assistant.metadata?.maxTurns?.toString() ?: "") }

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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name section
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Model section
            OutlinedTextField(
                value = editModel,
                onValueChange = { editModel = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Instruction section
            OutlinedTextField(
                value = editInstruction,
                onValueChange = { editInstruction = it },
                label = { Text("Instruction") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Description section
            OutlinedTextField(
                value = editDescription,
                onValueChange = { editDescription = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Max turns section
            OutlinedTextField(
                value = editMaxTurns,
                onValueChange = { editMaxTurns = it },
                label = { Text("Max Turns") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.updateMetadata(
                            id = assistant.id,
                            model = editModel.takeIf { it.isNotBlank() },
                            instruction = editInstruction.takeIf { it.isNotBlank() },
                            description = editDescription.takeIf { it.isNotBlank() },
                            maxTurns = editMaxTurns.toIntOrNull()
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes")
                }

                if (!assistant.metadata?.isPrimary!!) {
                    Button(
                        onClick = {
                            viewModel.setPrimaryAssistant(assistant.id)
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Set as Primary")
                    }
                }
            }

            // Display additional read-only information
            Text(
                text = "Created: ${assistant.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Last Updated: ${assistant.updatedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}