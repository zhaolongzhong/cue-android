package com.example.cue.ui.tools

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val toolsListRoute = "tools_list"

fun NavController.navigateToToolsList() {
    navigate(toolsListRoute)
}

fun NavGraphBuilder.toolsListScreen(onDismiss: () -> Unit) {
    composable(route = toolsListRoute) {
        ToolsListScreen(onDismiss = onDismiss)
    }
}