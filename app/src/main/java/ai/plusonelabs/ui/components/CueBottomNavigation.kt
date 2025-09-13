package ai.plusonelabs.ui.components

import ai.plusonelabs.navigation.Routes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CueBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Chat",
                )
            },
            label = { Text("Chat") },
            selected = currentRoute == Routes.CHAT,
            onClick = { onNavigate(Routes.CHAT) },
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Assistants",
                )
            },
            label = { Text("Assistants") },
            selected = currentRoute == Routes.ASSISTANTS,
            onClick = { onNavigate(Routes.ASSISTANTS) },
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                )
            },
            label = { Text("Settings") },
            selected = currentRoute == Routes.SETTINGS,
            onClick = { onNavigate(Routes.SETTINGS) },
        )
    }
}
