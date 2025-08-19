package com.example.cue.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.cue.navigation.Routes

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            selected = currentRoute == Routes.CHAT,
            onClick = {
                if (currentRoute != Routes.CHAT) {
                    navController.navigate(Routes.CHAT) {
                        popUpTo(Routes.CHAT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Folder, contentDescription = "Sessions") },
            label = { Text("Sessions") },
            selected = currentRoute == Routes.SESSION_LIST || currentRoute?.startsWith("session_chat") == true,
            onClick = {
                if (currentRoute != Routes.SESSION_LIST) {
                    navController.navigate(Routes.SESSION_LIST) {
                        popUpTo(Routes.CHAT)
                        launchSingleTop = true
                    }
                }
            },
        )
    }
}
