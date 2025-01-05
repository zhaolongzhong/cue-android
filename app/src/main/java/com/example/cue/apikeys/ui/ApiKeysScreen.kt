package com.example.cue.apikeys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.apikeys.models.ApiKey
import com.example.cue.apikeys.viewmodel.ApiKeysViewModel
import com.example.cue.common.ui.ErrorView
import com.example.cue.common.ui.LoadingView

@Composable
fun ApiKeysScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApiKeysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedApiKey by remember { mutableStateOf<ApiKey?>(null) }
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var showNewKeyDialog by remember { mutableStateOf<ApiKey?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Keys") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddKeyDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add API Key")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                uiState.isLoading && uiState.apiKeys.isEmpty() -> LoadingView()
                uiState.error != null -> ErrorView(
                    error = uiState.error!!,
                    onRetry = { viewModel.refresh() }
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.apiKeys) { apiKey ->
                            ApiKeyRow(
                                apiKey = apiKey,
                                onEdit = { selectedApiKey = apiKey },
                                onDelete = { viewModel.deleteKey(apiKey) }
                            )
                        }

                        if (uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Show add/edit key dialog
    if (showAddKeyDialog || selectedApiKey != null) {
        AddApiKeyDialog(
            apiKey = selectedApiKey,
            onDismiss = {
                showAddKeyDialog = false
                selectedApiKey = null
            },
            onSave = { name, secret ->
                if (selectedApiKey != null) {
                    viewModel.updateKey(selectedApiKey!!.id, name)
                } else {
                    viewModel.createKey(name, secret)
                }
                showAddKeyDialog = false
                selectedApiKey = null
            }
        )
    }

    // Show newly created key dialog
    showNewKeyDialog?.let { apiKey ->
        NewKeyCreatedDialog(
            apiKey = apiKey,
            onDismiss = { showNewKeyDialog = null }
        )
    }
}

@Composable
fun ApiKeyRow(
    apiKey: ApiKey,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCopyConfirmation by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = apiKey.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirmation = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = maskApiKey(apiKey.secret),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        // Copy to clipboard
                        showCopyConfirmation = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            apiKey.lastUsedAt?.let { lastUsed ->
                Text(
                    text = "Last used: ${formatRelativeTime(lastUsed)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: Text(
                text = "Never used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete API Key") },
            text = { Text("Are you sure you want to delete this API key? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCopyConfirmation) {
        AlertDialog(
            onDismissRequest = { showCopyConfirmation = false },
            title = { Text("API Key Copied") },
            text = { Text("The API key has been copied to your clipboard.") },
            confirmButton = {
                TextButton(onClick = { showCopyConfirmation = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private fun maskApiKey(key: String): String {
    return if (key.length > 8) {
        "${key.take(4)}...${key.takeLast(4)}"
    } else {
        key
    }
}

private fun formatRelativeTime(date: Date): String {
    // TODO: Implement relative time formatting
    return date.toString()
}