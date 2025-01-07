package com.example.cue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cue.navigation.CueNavigation
import com.example.cue.navigation.Routes
import com.example.cue.ui.components.CueBottomNavigation
import com.example.cue.ui.components.CueDrawer
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
                                )
                            ) {
                                TopAppBar(
                                    title = { Text(currentRoute.replaceFirstChar { it.uppercase() }) },
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
                            if (currentRoute in listOf(Routes.HOME, Routes.SETTINGS)) {
                                CueBottomNavigation(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        navController.navigate(route) {
                                            popUpTo(Routes.HOME) {
                                                inclusive = route == Routes.HOME
                                            }
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
