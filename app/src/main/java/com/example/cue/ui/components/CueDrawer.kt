package com.example.cue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.models.ClientStatus
import com.example.cue.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun CueDrawer(
    drawerState: DrawerState,
    currentRoute: String?,
    scope: CoroutineScope,
    onNavigate: (String) -> Unit,
    assistants: StateFlow<List<Assistant>>,
    selectedAssistantId: StateFlow<String?>,
    clientStatuses: StateFlow<Map<String, ClientStatus>>,
    onAssistantSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val assistantsList by assistants.collectAsState()
    val clientStatusesList by clientStatuses.collectAsState()
    val selectedId by selectedAssistantId.collectAsState()

    ModalDrawerSheet(modifier) {
        Column(
            modifier = Modifier.fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Assistants List
                Text(
                    "Your Assistants",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(assistantsList) { assistant ->
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(assistant.name)
                                    clientStatusesList.values.find { it.assistantId == assistant.id }
                                        ?.let { status ->
                                            if (status.isOnline) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            color = Color.Green,
                                                            shape = CircleShape,
                                                        ),
                                                )
                                            }
                                        }
                                }
                            },
                            selected = selectedId == assistant.id,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onAssistantSelected(assistant.id)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }

            DrawerItem(
                icon = Icons.Default.Chat,
                label = "Sessions",
                selected = currentRoute == Routes.SESSION_LIST || currentRoute?.startsWith("session_chat") == true,
                route = Routes.SESSION_LIST,
                drawerState = drawerState,
                scope = scope,
                onNavigate = onNavigate,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentRoute == Routes.SETTINGS,
                route = Routes.SETTINGS,
                drawerState = drawerState,
                scope = scope,
                onNavigate = onNavigate,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    route: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = {
            scope.launch {
                drawerState.close()
                onNavigate(route)
            }
        },
        modifier = modifier.padding(horizontal = 12.dp),
    )
}
