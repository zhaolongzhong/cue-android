package com.example.cue.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApiKeys: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var colorScheme by remember { mutableStateOf(ColorSchemeOption.SYSTEM) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Account Section
                    item {
                        SettingsSection(title = "Account") {
                            uiState.user?.let { user ->
                                SettingsRow(
                                    icon = Icons.Default.Email,
                                    title = "Email",
                                    subtitle = user.email
                                )
                            }
                        }
                    }

                    // Configuration Section
                    item {
                        SettingsSection(title = "Configuration") {
                            SettingsRow(
                                icon = Icons.Default.Key,
                                title = "API Keys",
                                onClick = onNavigateToApiKeys,
                                showChevron = true
                            )
                        }
                    }

                    // Appearance Section
                    item {
                        SettingsSection(title = "Appearance") {
                            SettingsRow(
                                icon = Icons.Default.WbSunny,
                                title = "Color Scheme",
                                trailing = {
                                    DropdownMenu(
                                        options = ColorSchemeOption.values().toList(),
                                        selectedOption = colorScheme,
                                        onOptionSelected = { colorScheme = it }
                                    )
                                }
                            )
                            SettingsRow(
                                icon = Icons.Default.Vibration,
                                title = "Haptic Feedback",
                                trailing = {
                                    Switch(
                                        checked = hapticFeedbackEnabled,
                                        onCheckedChange = { hapticFeedbackEnabled = it }
                                    )
                                }
                            )
                        }
                    }

                    // Access Token Section
                    item {
                        SettingsSection(title = "Access Token") {
                            TokenGenerationView(viewModel = viewModel)
                        }
                    }

                    // About Section
                    item {
                        SettingsSection(title = "About") {
                            SettingsRow(
                                icon = Icons.Default.Info,
                                title = "Version",
                                subtitle = "${uiState.version} (${uiState.buildNumber})"
                            )
                        }
                    }

                    // Logout Section
                    item {
                        SettingsSection {
                            SettingsRow(
                                icon = Icons.Default.ExitToApp,
                                title = "Log out",
                                onClick = { showLogoutDialog = true },
                                textColor = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutConfirmed()
                    }
                ) {
                    Text(
                        "Log Out",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
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
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = false,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
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
    options: List<ColorSchemeOption>,
    selectedOption: ColorSchemeOption,
    onOptionSelected: (ColorSchemeOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedOption.title)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TokenGenerationView(viewModel: SettingsViewModel) {
    var showGenerateDialog by remember { mutableStateOf(false) }

    SettingsRow(
        icon = Icons.Default.Token,
        title = "Generate Access Token",
        onClick = { showGenerateDialog = true }
    )

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate Access Token") },
            text = { Text("This will generate a new access token. The old token will be invalidated.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGenerateDialog = false
                        viewModel.generateAccessToken()
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}