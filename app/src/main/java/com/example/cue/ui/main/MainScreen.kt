package com.example.cue.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cue.R
import com.example.cue.ui.assistant.AssistantsScreen
import com.example.cue.ui.chat.ChatScreen
import com.example.cue.ui.settings.SettingsScreen

enum class TabScreen(val titleResId: Int) {
    CHAT(R.string.tab_chat),
    ASSISTANTS(R.string.tab_assistants),
    SETTINGS(R.string.tab_settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToAssistantDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(TabScreen.ASSISTANTS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedTab.titleResId)) },
                actions = {
                    if (selectedTab == TabScreen.CHAT || selectedTab == TabScreen.ASSISTANTS) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                if (!uiState.apiKey.isNullOrEmpty()) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Outlined.Chat, contentDescription = null) },
                        label = { Text(stringResource(TabScreen.CHAT.titleResId)) },
                        selected = selectedTab == TabScreen.CHAT,
                        onClick = { selectedTab = TabScreen.CHAT }
                    )
                }
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                    label = { Text(stringResource(TabScreen.ASSISTANTS.titleResId)) },
                    selected = selectedTab == TabScreen.ASSISTANTS,
                    onClick = { selectedTab = TabScreen.ASSISTANTS }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(TabScreen.SETTINGS.titleResId)) },
                    selected = selectedTab == TabScreen.SETTINGS,
                    onClick = { selectedTab = TabScreen.SETTINGS }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                TabScreen.CHAT -> {
                    if (!uiState.apiKey.isNullOrEmpty()) {
                        ChatScreen(
                            apiKey = uiState.apiKey
                        )
                    }
                }
                TabScreen.ASSISTANTS -> {
                    AssistantsScreen(
                        onAssistantClick = onNavigateToAssistantDetail
                    )
                }
                TabScreen.SETTINGS -> {
                    SettingsScreen()
                }
            }
        }
    }

    LaunchedEffect(uiState.currentUserId) {
        uiState.currentUserId?.let { userId ->
            viewModel.initializeWebSocket(userId)
        }
    }
}