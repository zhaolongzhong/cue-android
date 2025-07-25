package com.example.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cue.assistant.viewmodel.DrawerAssistantViewModel
import com.example.cue.navigation.CueNavigation
import com.example.cue.navigation.Routes
import com.example.cue.ui.components.CueDrawer
import com.example.cue.ui.theme.CueTheme
import com.example.cue.ui.theme.ThemeController
import com.example.cue.utils.AppLog
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeController: ThemeController
    private val appViewModel: AppViewModel by viewModels()
    private val drawerAssistantViewModel: DrawerAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.info("MainActivity onCreate() - Activity started")

        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    AppLog.info("MainActivity onStart() - App in foreground")
                    appViewModel.onAppForeground()
                }

                override fun onStop(owner: LifecycleOwner) {
                    AppLog.info("MainActivity onStop() - App in background")
                    appViewModel.onAppBackground()
                }
            },
        )
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
                        CueDrawer(
                            drawerState = drawerState,
                            currentRoute = currentRoute,
                            scope = scope,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Routes.HOME) {
                                        inclusive = route == Routes.HOME
                                    }
                                }
                            },
                            assistants = drawerAssistantViewModel.assistants,
                            selectedAssistantId = drawerAssistantViewModel.selectedAssistantId,
                            onAssistantSelected = { assistantId ->
                                navController.navigate(Routes.assistantChat(assistantId))
                                drawerAssistantViewModel.setSelectedAssistant(assistantId)
                            },
                            clientStatuses = appViewModel.clientStatuses,
                        )
                    },
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentRoute != null && currentRoute in listOf(
                                    Routes.HOME,
                                    Routes.ASSISTANTS,
                                    Routes.SETTINGS,
                                    Routes.ASSISTANT_CHAT,
                                )
                            ) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            when {
                                                currentRoute.startsWith(Routes.ASSISTANT_CHAT) -> "Chat"
                                                else -> currentRoute.replaceFirstChar { it.uppercase() }
                                            },
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                )
                            }
                        },
                    ) { innerPadding ->
                        CueNavigation(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
