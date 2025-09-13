package ai.plusonelabs.ui.session.components

import ai.plusonelabs.ui.session.CLIMode
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ModeSelector(
    currentMode: CLIMode,
    onModeSelect: (CLIMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select CLI Mode") },
        text = {
            Column {
                Text("Choose how you want to interact:")
                Spacer(modifier = Modifier.height(16.dp))

                CLIModeOption(
                    mode = CLIMode.CLAUDE_CODE,
                    currentMode = currentMode,
                    onModeSelect = onModeSelect,
                )

                Spacer(modifier = Modifier.height(8.dp))

                CLIModeOption(
                    mode = CLIMode.CUE_CLI,
                    currentMode = currentMode,
                    onModeSelect = onModeSelect,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun CLIModeOption(
    mode: CLIMode,
    currentMode: CLIMode,
    onModeSelect: (CLIMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = mode == currentMode

    Surface(
        onClick = { onModeSelect(mode) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            val (title, description) = when (mode) {
                CLIMode.CLAUDE_CODE -> Pair(
                    "Claude Code",
                    "Execute commands with file system access and tool usage",
                )
                CLIMode.CUE_CLI -> Pair(
                    "Cue CLI",
                    "Chat with AI assistant with full context awareness",
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
