package ai.plusonelabs.ui.session.components

import ai.plusonelabs.ui.session.CLISession
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionListItem(
    session: CLISession,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isStarred: Boolean = false,
    onToggleStar: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CLITypeIndicator(session = session)

                    Text(
                        text = session.customTitle ?: session.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )

                    if (isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Created ${timeAgo(session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    session.cwd?.let { cwd ->
                        Text(
                            text = "• $cwd",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusIndicator(isActive = session.isActive)

                    IconButton(
                        onClick = onToggleStar,
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isStarred) "Unstar" else "Star",
                            tint = if (isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                val messageCount = session.sessionInfo?.messageCount ?: 0
                if (messageCount > 0) {
                    Text(
                        text = "$messageCount msgs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CLITypeIndicator(
    session: CLISession,
    modifier: Modifier = Modifier,
) {
    val isClaudeCode = isClaudeCodeSession(session)
    val (text, color) = if (isClaudeCode) {
        "Claude" to Color(0xFF2196F3) // Blue
    } else {
        "Cue" to Color(0xFF9C27B0) // Purple
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun StatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(8.dp),
        shape = RoundedCornerShape(50),
        color = if (isActive) Color.Green else Color.Red,
    ) {}
}

private fun timeAgo(date: Date): String {
    val now = Date()
    val diffMillis = now.time - date.time

    return when {
        diffMillis < 1.minutes.inWholeMilliseconds -> "just now"
        diffMillis < 1.hours.inWholeMilliseconds -> {
            val minutes = (diffMillis / 1.minutes.inWholeMilliseconds).toInt()
            "${minutes}m ago"
        }
        diffMillis < 1.days.inWholeMilliseconds -> {
            val hours = (diffMillis / 1.hours.inWholeMilliseconds).toInt()
            "${hours}h ago"
        }
        else -> {
            val days = (diffMillis / 1.days.inWholeMilliseconds).toInt()
            "${days}d ago"
        }
    }
}

private fun isClaudeCodeSession(session: CLISession): Boolean {
    // Fallback to detection logic based on available fields
    val clientName = session.targetClientName?.lowercase() ?: ""
    val clientId = session.targetClientId?.lowercase() ?: ""

    return clientName.contains("claude") ||
        clientName.contains("anthropic") ||
        clientId.contains("claude") ||
        clientId.contains("anthropic")
}
