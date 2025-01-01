package com.example.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cue.auth.ui.LoginScreen
import com.example.cue.auth.ui.SignUpScreen
import com.example.cue.openai.OpenAIChatScreen
import com.example.cue.settings.SettingsScreen
import com.example.cue.settings.apikeys.ApiKeysScreen
import com.example.cue.ui.theme.CueTheme
import com.example.cue.ui.theme.ThemeController
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeController: ThemeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CueTheme(themeController = themeController) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Cue Assistant",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Divider()
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                                label = { Text("Conversations") },
                                selected = currentRoute == "home",
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                label = { Text("History") },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        // TODO: Add history navigation
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Settings") },
                                selected = currentRoute == "settings",
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        navController.navigate("settings") {
                                            popUpTo("home")
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    },
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentRoute in listOf("home", "settings")) {
                                TopAppBar(
                                    title = { Text(if (currentRoute == "home") "Chat" else "Settings") },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                )
                            }
                        },
                        bottomBar = {
                            // Only show bottom navigation when user is authenticated
                            if (currentRoute in listOf("home", "settings")) {
                                NavigationBar {
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                Icons.Default.Home,
                                                contentDescription = "Chat",
                                            )
                                        },
                                        label = { Text("Chat") },
                                        selected = currentRoute == "home",
                                        onClick = {
                                            navController.navigate("home") {
                                                popUpTo("home") { inclusive = true }
                                            }
                                        },
                                    )
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Settings",
                                            )
                                        },
                                        label = { Text("Settings") },
                                        selected = currentRoute == "settings",
                                        onClick = {
                                            navController.navigate("settings") {
                                                popUpTo("home")
                                            }
                                        },
                                    )
                                }
                            }
                        },
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onNavigateToSignUp = {
                                        navController.navigate("signup")
                                    },
                                    onLoginSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                )
                            }
                            composable("signup") {
                                SignUpScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                    onSignUpSuccess = {
                                        navController.navigate("home") {
                                            popUpTo("signup") { inclusive = true }
                                        }
                                    },
                                )
                            }
                            composable("home") {
                                OpenAIChatScreen(
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onNavigateToApiKeys = {
                                        navController.navigate("api_keys")
                                    },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                )
                            }
                            composable("api_keys") {
                                ApiKeysScreen(
                                    onNavigateBack = {
                                        navController.popBackStack()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
