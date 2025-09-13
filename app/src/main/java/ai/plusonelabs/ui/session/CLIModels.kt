package ai.plusonelabs.ui.session

import java.util.Date
import java.util.UUID

enum class CLIMode {
    CLAUDE_CODE,
    CUE_CLI,
}

enum class CLIMessageType {
    COMMAND,
    RESPONSE,
    STATUS,
    ERROR,
}

enum class CLIStatus {
    RECEIVED,
    VALIDATED,
    EXECUTING,
    COMPLETED,
    ERROR,
}

data class CLIClient(
    val clientId: String,
    val shortName: String,
    val displayName: String,
    val platform: String,
    val hostname: String? = null,
    val cwd: String? = null,
    val isOnline: Boolean = true,
    val connectedAt: Date = Date(),
    val lastSeen: Date? = null,
    val agentInfo: AgentInfo? = null,
)

data class AgentInfo(
    val model: String,
    val maxTurns: Int,
    val provider: String,
    val capabilities: List<String> = emptyList(),
    val version: String,
    val cliVersion: String? = null,
)

data class CLISession(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val cwd: String,
    val createdAt: Date = Date(),
    val isActive: Boolean = true,
    val messages: List<CLIMessage> = emptyList(),
    val lastUpdated: Date = Date(),
    val targetClientId: String? = null,
    val targetClientName: String? = null,
    val sessionInfo: SessionInfo? = null,
    val customTitle: String? = null,
    val isStarred: Boolean = false,
    val isArchived: Boolean = false,
)

data class SessionInfo(
    val sessionId: String,
    val clientId: String,
    val messageCount: Int = 0,
    val maxMessages: Int = 20,
    val hasContext: Boolean = false,
    val currentModel: String? = null,
    val currentProvider: String? = null,
    val agent: AgentConfig? = null,
)

data class AgentConfig(
    val model: String,
    val maxTurns: Int,
    val systemPrompt: String? = null,
)

data class CLIMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: CLIMessageType,
    val content: String,
    val timestamp: Date = Date(),
    val status: CLIStatus? = null,
    val step: String? = null,
    val requestId: String? = null,
    val sessionId: String? = null,
)
