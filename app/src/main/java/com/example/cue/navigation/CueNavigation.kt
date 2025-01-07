package com.example.cue.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.cue.assistant.ui.AssistantScreen
import com.example.cue.auth.ui.LoginScreen
import com.example.cue.auth.ui.SignUpScreen
import com.example.cue.openai.OpenAIChatScreen
import com.example.cue.settings.SettingsScreen
import com.example.cue.settings.apikeys.ApiKeysScreen

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ASSISTANTS = "assistants"
    const val API_KEYS = "api_keys"
}

@Composable
fun CueNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier,
    ) {
        authGraph(navController)
        mainGraph(navController)
    }
}

fun NavGraphBuilder.authGraph(navController: NavController) {
    composable(Routes.LOGIN) {
        LoginScreen(
            onNavigateToSignUp = {
                navController.navigate(Routes.SIGNUP)
            },
            onLoginSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.SIGNUP) {
        SignUpScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
            onSignUpSuccess = {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SIGNUP) { inclusive = true }
                }
            },
        )
    }
}

fun NavGraphBuilder.mainGraph(navController: NavController) {
    composable(Routes.HOME) {
        OpenAIChatScreen()
    }
    composable(Routes.SETTINGS) {
        SettingsScreen(
            onNavigateToApiKeys = {
                navController.navigate(Routes.API_KEYS)
            },
            onLogout = {
                navController.navigate(Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            },
        )
    }
    composable(Routes.ASSISTANTS) {
        AssistantScreen(onAssistantClick = {})
    }
    composable(Routes.API_KEYS) {
        ApiKeysScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
}
