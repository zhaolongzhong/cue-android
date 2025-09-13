package ai.plusonelabs.navigation

import ai.plusonelabs.AppViewModel
import ai.plusonelabs.assistant.ui.AssistantScreen
import ai.plusonelabs.auth.ui.LoginScreen
import ai.plusonelabs.auth.ui.SignUpScreen
import ai.plusonelabs.chat.AssistantChatScreen
import ai.plusonelabs.chat.UnifiedChatScreen
import ai.plusonelabs.debug.DebugScreen
import ai.plusonelabs.settings.SettingsScreen
import ai.plusonelabs.settings.apikeys.ApiKeysScreen
import ai.plusonelabs.ui.session.SessionChatScreen
import ai.plusonelabs.ui.session.SessionListScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val ASSISTANTS = "assistants"
    const val API_KEYS = "api_keys"
    const val ASSISTANT_CHAT = "assistant_chat/{assistantId}"
    const val DEBUG = "debug"
    const val SESSION_LIST = "session_list"
    const val SESSION_CHAT = "session_chat/{sessionId}"
    fun assistantChat(assistantId: String) = "assistant_chat/$assistantId"
    fun sessionChat(sessionId: String) = "session_chat/$sessionId"
}

@Composable
fun CueNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Routes.CHAT else Routes.LOGIN,
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
                navController.navigate(Routes.CHAT) {
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
                navController.navigate(Routes.CHAT) {
                    popUpTo(Routes.SIGNUP) { inclusive = true }
                }
            },
        )
    }
}

fun NavGraphBuilder.mainGraph(navController: NavController) {
    composable(Routes.CHAT) {
        UnifiedChatScreen()
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
            onNavigateToDebug = {
                navController.navigate(Routes.DEBUG)
            },
        )
    }
    composable(
        route = Routes.ASSISTANT_CHAT,
        arguments = listOf(navArgument("assistantId") { type = NavType.StringType }),
    ) {
        AssistantChatScreen()
    }

    composable(Routes.ASSISTANTS) {
        AssistantScreen(onAssistantClick = {
            navController.navigate(Routes.assistantChat(it.id))
        })
    }
    composable(Routes.API_KEYS) {
        ApiKeysScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
    composable(Routes.DEBUG) {
        DebugScreen(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }
    composable(Routes.SESSION_LIST) {
        SessionListScreen(
            onNavigateToChat = { session ->
                navController.navigate(Routes.sessionChat(session.id))
            },
        )
    }
    composable(
        route = Routes.SESSION_CHAT,
        arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
        // TODO: Get session by ID from viewModel or repository
        // For now, create a mock session
        val mockSession = ai.plusonelabs.ui.session.CLISession(
            id = sessionId,
            displayName = "Mock Session",
            cwd = "/Users",
            createdAt = java.util.Date(),
            isActive = true,
            targetClientId = "mock-client",
            targetClientName = "Mock Client",
        )
        SessionChatScreen(session = mockSession)
    }
}
