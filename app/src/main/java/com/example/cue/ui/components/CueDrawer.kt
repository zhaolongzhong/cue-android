package com.example.cue.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cue.navigation.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun CueDrawer(
    drawerState: DrawerState,
    currentRoute: String?,
    scope: CoroutineScope,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Cue Assistant",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Divider()

        DrawerItem(
            icon = Icons.Default.Chat,
            label = "Conversations",
            selected = currentRoute == Routes.HOME,
            route = Routes.HOME,
            drawerState = drawerState,
            scope = scope,
            onNavigate = onNavigate,
        )

        DrawerItem(
            icon = Icons.Default.Person,
            label = "Assistants",
            selected = currentRoute == Routes.ASSISTANTS,
            route = Routes.ASSISTANTS,
            drawerState = drawerState,
            scope = scope,
            onNavigate = onNavigate,
        )

        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = currentRoute == Routes.SETTINGS,
            route = Routes.SETTINGS,
            drawerState = drawerState,
            scope = scope,
            onNavigate = onNavigate,
        )
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
