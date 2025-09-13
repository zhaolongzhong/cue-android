package ai.plusonelabs.settings

import ai.plusonelabs.settings.views.ColorThemeDropdownMenu
import ai.plusonelabs.settings.views.SettingsRow
import ai.plusonelabs.settings.views.SettingsSection
import ai.plusonelabs.settings.views.TokenGenerationView
import ai.plusonelabs.ui.theme.ColorSchemeOption
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToApiKeys: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToDebug: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Account Section
                    item {
                        SettingsSection(title = "Account") {
                            uiState.user?.let { user ->
                                SettingsRow(
                                    icon = Icons.Outlined.Email,
                                    title = "Email",
                                    subtitle = user.email,
                                )
                            }
                        }
                    }

                    // Configuration Section
                    item {
                        SettingsSection(title = "Configuration") {
                            SettingsRow(
                                icon = Icons.Outlined.Key,
                                title = "API Keys",
                                onClick = onNavigateToApiKeys,
                                showChevron = true,
                            )
                        }
                    }

                    // Debug Section (only show if callback is provided)
                    onNavigateToDebug?.let { debugCallback ->
                        item {
                            SettingsSection(title = "Debug") {
                                SettingsRow(
                                    icon = Icons.Default.BugReport,
                                    title = "AI Provider Settings",
                                    subtitle = "Change AI provider (Anthropic, OpenAI, Gemini, Cue)",
                                    onClick = debugCallback,
                                    showChevron = true,
                                )
                            }
                        }
                    }

                    // Appearance Section
                    item {
                        SettingsSection(title = "Appearance") {
                            SettingsRow(
                                icon = Icons.Filled.WbSunny,
                                title = "Color Scheme",
                                trailing = {
                                    ColorThemeDropdownMenu(
                                        options = ColorSchemeOption.entries,
                                        selectedOption = uiState.theme,
                                        onOptionSelected = { viewModel.setTheme(it) },
                                    )
                                },
                            )
                            SettingsRow(
                                icon = Icons.Outlined.Vibration,
                                title = "Haptic Feedback",
                                trailing = {
                                    Switch(
                                        checked = hapticFeedbackEnabled,
                                        onCheckedChange = { hapticFeedbackEnabled = it },
                                        modifier = Modifier.height(24.dp),
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    )
                                },
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
                                icon = Icons.Outlined.Info,
                                title = "Version",
                                subtitle = "${uiState.version} (${uiState.buildNumber})",
                            )
                        }
                    }

                    // Logout Section
                    item {
                        SettingsSection {
                            SettingsRow(
                                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                                title = "Log out",
                                onClick = { showLogoutDialog = true },
                                textColor = MaterialTheme.colorScheme.error,
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
                        viewModel.logout()
                        onLogout()
                    },
                ) {
                    Text(
                        "Log Out",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}
