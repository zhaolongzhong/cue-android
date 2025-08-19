package com.example.cue.ui.session.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cue.ui.session.CLIMode
import com.example.cue.ui.session.CLISession

@Composable
fun SessionHeader(
    session: CLISession,
    cliMode: CLIMode,
    availableClients: List<com.example.cue.ui.session.CLIClient>,
    selectedClientId: String?,
    connectionStatus: String,
    onModeClick: () -> Unit,
    onClientClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionInfo(
                    session = session,
                    modifier = Modifier.weight(1f),
                )

                ControlButtons(
                    cliMode = cliMode,
                    availableClients = availableClients,
                    selectedClientId = selectedClientId,
                    onModeClick = onModeClick,
                    onClientClick = onClientClick,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ConnectionStatus(
                connectionStatus = connectionStatus,
                clientCount = availableClients.size,
            )
        }
    }
}

@Composable
private fun SessionInfo(
    session: CLISession,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "CLI Session",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Session: ${session.id.take(12)}...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Working Dir: ${session.cwd}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ControlButtons(
    cliMode: CLIMode,
    availableClients: List<com.example.cue.ui.session.CLIClient>,
    selectedClientId: String?,
    onModeClick: () -> Unit,
    onClientClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mode selector button
        Button(
            onClick = onModeClick,
            modifier = Modifier.height(32.dp),
        ) {
            Text(
                text = when (cliMode) {
                    CLIMode.CLAUDE_CODE -> "Claude Code"
                    CLIMode.CUE_CLI -> "Cue CLI"
                },
                fontSize = 12.sp,
            )
        }

        // Client selector button
        Button(
            onClick = onClientClick,
            modifier = Modifier.height(32.dp),
        ) {
            Text(
                text = if (selectedClientId != null) {
                    availableClients.find { it.clientId == selectedClientId }?.shortName ?: "Client"
                } else {
                    "Select Client"
                },
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ConnectionStatus(
    connectionStatus: String,
    clientCount: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val statusColor = when (connectionStatus) {
            "connected" -> MaterialTheme.colorScheme.primary
            "connecting" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }

        Surface(
            shape = RoundedCornerShape(4.dp),
            color = statusColor.copy(alpha = 0.1f),
        ) {
            Text(
                text = connectionStatus.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        if (clientCount > 0) {
            Text(
                text = "• $clientCount clients available",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
