package com.example.cue.settings.apikeys.ui

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.auth.AuthError
import com.example.cue.settings.apikeys.ServerApiKeysViewModel
import com.example.cue.settings.apikeys.models.ApiKey
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ServerApiKeysScreen(
    viewModel: ServerApiKeysViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    
    var showDeleteConfirm by remember { mutableStateOf<ApiKey?>(null) }
    var showEditDialog by remember { mutableStateOf<ApiKey?>(null) }
    var editName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Keys") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Edit, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAddKeyDialog(true) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Key")
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
            if (uiState.isLoading && uiState.apiKeys.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.apiKeys) { key ->
                    ApiKeyItem(
                        apiKey = key,
                        onCopy = { secretToCopy ->
                            clipboardManager.setText(AnnotatedString(secretToCopy))
                        },
                        onEdit = {
                            editName = key.name
                            showEditDialog = key
                        },
                        onDelete = {
                            showDeleteConfirm = key
                        },
                        onLoadMore = { viewModel.loadMoreIfNeeded(it) }
                    )
                }
            }

            uiState.error?.let { error ->
                ErrorSnackbar(
                    error = error,
                    onDismiss = { viewModel.refresh() }
                )
            }

            if (uiState.isShowingAddKey) {
                AddKeyDialog(
                    onDismiss = { viewModel.toggleAddKeyDialog(false) },
                    onAdd = { name -> viewModel.createNewApiKey(name) }
                )
            }

            showEditDialog?.let { key ->
                EditKeyDialog(
                    name = editName,
                    onNameChange = { editName = it },
                    onDismiss = { showEditDialog = null },
                    onSave = {
                        viewModel.updateKey(key, editName)
                        showEditDialog = null
                    }
                )
            }

            showDeleteConfirm?.let { key ->
                DeleteConfirmationDialog(
                    onConfirm = {
                        viewModel.deleteKey(key)
                        showDeleteConfirm = null
                    },
                    onDismiss = { showDeleteConfirm = null }
                )
            }

            uiState.newKeyCreated?.let { newKey ->
                NewKeyDialog(
                    apiKey = newKey,
                    onDismiss = { viewModel.clearNewKeyCreated() },
                    onCopy = { secretToCopy ->
                        clipboardManager.setText(AnnotatedString(secretToCopy))
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiKeyItem(
    apiKey: ApiKey,
    onCopy: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLoadMore: (ApiKey) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LaunchedEffect(apiKey) {
        onLoadMore(apiKey)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(apiKey.secret) }
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
                    text = apiKey.name,
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
                text = "Created: ${dateFormatter.format(apiKey.createdAt)}",
                style = MaterialTheme.typography.bodyMedium
            )

            apiKey.lastUsedAt?.let { lastUsed ->
                Text(
                    text = "Last used: ${dateFormatter.format(lastUsed)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            apiKey.expiresAt?.let { expires ->
                Text(
                    text = "Expires: ${dateFormatter.format(expires)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = "Status: ${if (apiKey.isActive) "Active" else "Inactive"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AddKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New API Key") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Key Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name) }) {
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
private fun EditKeyDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit API Key") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Key Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) {
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

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete API Key") },
        text = { Text("Are you sure you want to delete this API key? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
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
private fun NewKeyDialog(
    apiKey: ApiKeyPrivate,
    onDismiss: () -> Unit,
    onCopy: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Key Created") },
        text = {
            Column {
                Text(
                    "Your new API key has been created. Please copy and save your key now, as you won't be able to see it again.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = apiKey.secret,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onCopy(apiKey.secret)
                onDismiss()
            }) {
                Text("Copy and Close")
            }
        }
    )
}

@Composable
private fun ErrorSnackbar(
    error: AuthError,
    onDismiss: () -> Unit
) {
    Snackbar(
        action = {
            TextButton(onClick = onDismiss) {
                Text("Retry")
            }
        },
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            when (error) {
                AuthError.Unauthorized -> "Session expired. Please log in again."
                AuthError.InvalidResponse -> "Invalid response from server."
                AuthError.NetworkError -> "Network error. Please check your connection."
                else -> "An unexpected error occurred."
            }
        )
    }
}