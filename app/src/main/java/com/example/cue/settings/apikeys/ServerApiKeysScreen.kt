package com.example.cue.settings.apikeys

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cue.R
import com.example.cue.settings.apikeys.views.ServerApiKeyRow
import com.example.cue.settings.views.CreateApiKeyDialog
import com.example.cue.settings.views.ErrorView
import com.example.cue.settings.views.LoadingView
import com.example.cue.settings.views.NewApiKeyDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerApiKeysScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServerApiKeysViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            snackbarHostState.showSnackbar(
                message = "API key copied to clipboard",
                duration = SnackbarDuration.Short
            )
            showCopiedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.api_keys)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleAddKeyDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_api_key)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.apiKeys.isEmpty() -> {
                    LoadingView()
                }
                uiState.error != null && uiState.apiKeys.isEmpty() -> {
                    ErrorView(
                        error = uiState.error,
                        onRetry = viewModel::refresh
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.apiKeys,
                            key = { it.id }
                        ) { key ->
                            ServerApiKeyRow(
                                key = key,
                                onUpdate = viewModel::updateKey,
                                onDelete = viewModel::deleteKey,
                                modifier = Modifier.fillMaxWidth()
                            )
                            viewModel.loadMoreIfNeeded(key)
                        }
                    }
                }
            }

            // Loading indicator for pagination or operations
            AnimatedVisibility(
                visible = uiState.isLoading && uiState.apiKeys.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 3.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(24.dp)
                    )
                }
            }
        }

        if (uiState.isShowingAddKey) {
            CreateApiKeyDialog(
                onDismiss = { viewModel.toggleAddKeyDialog(false) },
                onConfirm = { name ->
                    viewModel.createNewApiKey(name)
                }
            )
        }

        uiState.newKeyCreated?.let { newKey ->
            NewApiKeyDialog(
                key = newKey,
                onDismiss = {
                    viewModel.clearNewKeyCreated()
                    clipboardManager.setText(AnnotatedString(newKey.secret))
                    showCopiedSnackbar = true
                }
            )
        }
    }
}