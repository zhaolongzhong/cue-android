package com.example.cue.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cue.ui.session.components.EmptySessionsView
import com.example.cue.ui.session.components.SessionListItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.cue.utils.AppLog as Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onNavigateToChat: (CLISession) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionListViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val managerClient by viewModel.managerClient.collectAsStateWithLifecycle()
    val starredSessionIds by viewModel.starredSessionIds.collectAsStateWithLifecycle()

    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var showClientsBottomSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    var showRenameDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<CLISession?>(null) }
    var renameText by remember { mutableStateOf("") }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Debug logging
    Log.d("SessionListScreen", "Recomposing: showClientsBottomSheet = $showClientsBottomSheet")

    // Connect to WebSocket when screen appears
    DisposableEffect(Unit) {
        Log.d("SessionListScreen", "Screen appeared, starting discovery")
        viewModel.startDiscovery()
        onDispose {
            Log.d("SessionListScreen", "Screen disposing, stopping discovery")
            viewModel.stopDiscovery()
        }
    }

    // Log current state for debugging
    Log.d("SessionListScreen", "Sessions: ${sessions.size}, Manager: ${managerClient != null}, Connection: $connectionStatus")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (managerClient != null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateSessionDialog = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create session",
                        )
                    },
                    text = { Text("New Session") },
                )
            }
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.refreshClients()
                    viewModel.startDiscovery()
                    delay(1500) // Give time for discovery
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                if (sessions.isEmpty()) {
                    EmptySessionsView(
                        hasManagerClient = managerClient != null,
                        onCreateSession = {
                            Log.d("SessionListScreen", "onCreateSession clicked")
                            showCreateSessionDialog = true
                        },
                        onViewClients = {
                            Log.d("SessionListScreen", "onViewClients clicked, setting showClientsBottomSheet = true")
                            showClientsBottomSheet = true
                        },
                        connectionStatus = connectionStatus,
                        modifier = Modifier.padding(top = 60.dp),
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        items(sessions) { session ->
                            var showContextMenu by remember { mutableStateOf(false) }

                            SessionListItem(
                                session = session,
                                onClick = {
                                    Log.d("SessionListScreen", "Session clicked: ${session.id} - ${session.displayName}")
                                    onNavigateToChat(session)
                                },
                                onRemove = {
                                    Log.d("SessionListScreen", "Delete clicked for session: ${session.id} - ${session.displayName}")
                                    viewModel.removeSession(session)
                                },
                                isStarred = starredSessionIds.contains(session.id),
                                onToggleStar = {
                                    Log.d("SessionListScreen", "Star clicked for session: ${session.id}")
                                    viewModel.toggleSessionStar(session)
                                },
                                onLongClick = {
                                    Log.d("SessionListScreen", "Long press on session: ${session.id}")
                                    showContextMenu = true
                                },
                            )

                            // Context menu dropdown
                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { showContextMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (starredSessionIds.contains(session.id)) "Unstar" else "Star")
                                    },
                                    onClick = {
                                        showContextMenu = false
                                        viewModel.toggleSessionStar(session)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        showContextMenu = false
                                        sessionToRename = session
                                        renameText = session.customTitle ?: session.displayName
                                        showRenameDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Archive") },
                                    onClick = {
                                        showContextMenu = false
                                        viewModel.archiveSession(session)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        showContextMenu = false
                                        viewModel.removeSession(session)
                                    },
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Create Session Dialog
    if (showCreateSessionDialog) {
        CreateSessionDialog(
            onDismiss = { showCreateSessionDialog = false },
            onCreate = { workingDirectory, isClaudeCodeMode ->
                val newSession = viewModel.createSession(workingDirectory, isClaudeCodeMode)
                showCreateSessionDialog = false
                newSession?.let { onNavigateToChat(it) }
            },
        )
    }

    // Clients Bottom Sheet
    if (showClientsBottomSheet) {
        Log.d("SessionListScreen", "Showing clients bottom sheet with ${viewModel.availableClients.collectAsStateWithLifecycle().value.size} clients")
        ModalBottomSheet(
            onDismissRequest = { showClientsBottomSheet = false },
            sheetState = bottomSheetState,
        ) {
            AvailableClientsSheet(
                availableClients = viewModel.availableClients.collectAsStateWithLifecycle().value,
                isDiscovering = viewModel.isDiscovering.collectAsStateWithLifecycle().value,
                onRefresh = { viewModel.startDiscovery() },
                onClose = { showClientsBottomSheet = false },
            )
        }
    }

    // Rename Dialog
    if (showRenameDialog && sessionToRename != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showRenameDialog = false
                sessionToRename = null
            },
            title = { Text("Rename Session") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Session Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        sessionToRename?.let { session ->
                            viewModel.renameSession(session, renameText)
                        }
                        showRenameDialog = false
                        sessionToRename = null
                    },
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showRenameDialog = false
                        sessionToRename = null
                    },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onCreate: (workingDirectory: String, isClaudeCodeMode: Boolean) -> Unit,
) {
    var workingDirectory by remember { mutableStateOf("/Users") }
    var isClaudeCodeMode by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Session") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = workingDirectory,
                    onValueChange = { workingDirectory = it },
                    label = { Text("Working Directory") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.material3.Switch(
                        checked = isClaudeCodeMode,
                        onCheckedChange = { isClaudeCodeMode = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isClaudeCodeMode) "Claude Code Mode" else "Cue CLI Mode",
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onCreate(workingDirectory, isClaudeCodeMode) },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AvailableClientsSheet(
    availableClients: List<CLIClient>,
    isDiscovering: Boolean,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Available CLI Clients",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "${availableClients.size} client${if (availableClients.size != 1) "s" else ""} found",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            androidx.compose.material3.IconButton(
                onClick = onRefresh,
                enabled = !isDiscovering,
            ) {
                if (isDiscovering) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (availableClients.isEmpty()) {
            EmptyClientsState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(availableClients) { client ->
                    ClientCard(client = client)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Close")
        }
    }
}

@Composable
private fun ClientCard(client: CLIClient) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = client.displayName,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = client.platform.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ID: ${client.clientId}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            )

            client.hostname?.let {
                Text(
                    text = "Host: $it",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }

            client.cwd?.let {
                Text(
                    text = "Working Dir: $it",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyClientsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Computer,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No CLI clients found",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Make sure CLI clients are running and connected",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
