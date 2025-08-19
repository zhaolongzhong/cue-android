package com.example.cue.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cue.ui.session.CLIMessage
import com.example.cue.ui.session.CLIMessageType
import com.example.cue.ui.session.CLIMode
import com.example.cue.ui.session.CLIStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessageList(
    messages: List<CLIMessage>,
    cliMode: CLIMode,
    isLoading: Boolean,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (messages.isEmpty()) {
            item {
                WelcomeMessage(cliMode = cliMode)
            }
        } else {
            items(messages) { message ->
                CLIChatMessage(message = message, cliMode = cliMode)
            }
        }

        if (isLoading) {
            item {
                LoadingIndicator()
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun WelcomeMessage(
    cliMode: CLIMode,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            val (title, description, examples) = when (cliMode) {
                CLIMode.CLAUDE_CODE -> Triple(
                    "Claude Code Session Ready",
                    "Execute commands with file system access and tool usage",
                    listOf(
                        "/cc list files in current directory",
                        "/cc create a new Python script",
                        "/cc check git status",
                    ),
                )
                CLIMode.CUE_CLI -> Triple(
                    "Cue CLI Chat Ready",
                    "Chat with AI assistant with full context awareness",
                    listOf(
                        "What files are in this directory?",
                        "Help me write a Python script",
                        "Explain this codebase structure",
                    ),
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Try these examples:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            examples.forEach { example ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CLIChatMessage(
    message: CLIMessage,
    cliMode: CLIMode,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (message.type) {
        CLIMessageType.COMMAND -> MaterialTheme.colorScheme.primaryContainer
        CLIMessageType.RESPONSE -> MaterialTheme.colorScheme.secondaryContainer
        CLIMessageType.STATUS -> MaterialTheme.colorScheme.tertiaryContainer
        CLIMessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

    val alignment = when (message.type) {
        CLIMessageType.COMMAND -> Alignment.End
        else -> Alignment.Start
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) {
        dateFormat.format(message.timestamp)
    }

    val messageIcon = when (message.type) {
        CLIMessageType.COMMAND -> "$ "
        CLIMessageType.RESPONSE -> "🤖 "
        CLIMessageType.STATUS -> when (message.status) {
            CLIStatus.RECEIVED -> "📥 "
            CLIStatus.VALIDATED -> "✅ "
            CLIStatus.EXECUTING -> "⚙️ "
            CLIStatus.COMPLETED -> "✔️ "
            CLIStatus.ERROR -> "❌ "
            else -> "📋 "
        }
        CLIMessageType.ERROR -> "❌ "
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .padding(
                    start = if (message.type == CLIMessageType.COMMAND) 48.dp else 0.dp,
                    end = if (message.type == CLIMessageType.COMMAND) 0.dp else 48.dp,
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = messageIcon,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }
        }
    }
}
