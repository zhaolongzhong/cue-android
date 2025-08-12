package com.example.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cue.assistant.viewmodel.DrawerAssistantViewModel
import com.example.cue.debug.DebugBottomSheetContent
import com.example.cue.debug.DebugViewModel
import com.example.cue.debug.Provider
import com.example.cue.navigation.CueNavigation
import com.example.cue.navigation.Routes
import com.example.cue.ui.components.CueDrawer
import com.example.cue.ui.theme.CueTheme
import com.example.cue.ui.theme.ThemeController
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import com.example.cue.utils.AppLog as Log

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeController: ThemeController
    private val appViewModel: AppViewModel by viewModels()
    private val drawerAssistantViewModel: DrawerAssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate() - Activity started")

        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    Log.i("MainActivity", "onStart() - App in foreground")
                    appViewModel.onAppForeground()
                }

                override fun onStop(owner: LifecycleOwner) {
                    Log.i("MainActivity", "onStop() - App in background")
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

                val debugViewModel: DebugViewModel = hiltViewModel()
                val debugUiState by debugViewModel.uiState.collectAsStateWithLifecycle()

                var showDebugBottomSheet by remember { mutableStateOf(false) }
                val bottomSheetState = rememberModalBottomSheetState(
                    skipPartiallyExpanded = true,
                )

                LaunchedEffect(Unit) {
                    debugViewModel.loadCurrentProvider()
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        CueDrawer(
                            drawerState = drawerState,
                            currentRoute = currentRoute,
                            scope = scope,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(Routes.CHAT) {
                                        inclusive = route == Routes.CHAT
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
                                    Routes.CHAT,
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
                                                currentRoute == Routes.CHAT -> "${debugUiState.currentProvider.displayName} Chat"
                                                else -> currentRoute.replaceFirstChar { it.uppercase() }
                                            },
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { showDebugBottomSheet = true }) {
                                            Icon(
                                                imageVector = Icons.Default.BugReport,
                                                contentDescription = "AI Provider Settings",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
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

                if (showDebugBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showDebugBottomSheet = false },
                        sheetState = bottomSheetState,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                    ) {
                        DebugBottomSheetContent(
                            viewModel = debugViewModel,
                            onProviderSelected = { provider ->
                                showDebugBottomSheet = false
                                val targetRoute = when (provider) {
                                    Provider.OPENAI -> Routes.CHAT
                                    Provider.ANTHROPIC -> Routes.CHAT
                                    Provider.CUE -> Routes.CHAT
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
