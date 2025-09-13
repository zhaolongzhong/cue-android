package ai.plusonelabs.ui.session.managers

import ai.plusonelabs.ui.session.CLIMode
import ai.plusonelabs.ui.session.CLISession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import ai.plusonelabs.utils.AppLog as Log

class SessionManager {
    companion object {
        private const val TAG = "SessionManager"
    }

    private val _currentSession = MutableStateFlow<CLISession?>(null)
    val currentSession: StateFlow<CLISession?> = _currentSession.asStateFlow()

    private val _cliMode = MutableStateFlow(CLIMode.CLAUDE_CODE)
    val cliMode: StateFlow<CLIMode> = _cliMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var claudeCodeSessionId: String? = null

    fun createNewSession(selectedClientId: String? = null, selectedClientName: String? = null): CLISession {
        val sessionId = "android-session-${UUID.randomUUID().toString().take(8)}"
        val workingDirectory = getDefaultWorkingDirectory()

        val session = CLISession(
            id = sessionId,
            displayName = "CLI Session",
            cwd = workingDirectory,
            targetClientId = selectedClientId,
            targetClientName = selectedClientName,
        )

        _currentSession.value = session
        Log.d(TAG, "Created new session: $sessionId")
        return session
    }

    fun toggleMode(): CLIMode {
        _cliMode.value = when (_cliMode.value) {
            CLIMode.CLAUDE_CODE -> CLIMode.CUE_CLI
            CLIMode.CUE_CLI -> CLIMode.CLAUDE_CODE
        }

        Log.d(TAG, "Switched to ${_cliMode.value} mode")
        return _cliMode.value
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun updateClaudeSessionId(sessionId: String?) {
        claudeCodeSessionId = sessionId
        Log.d(TAG, "Updated Claude session ID: $sessionId")
    }

    fun getClaudeSessionId(): String? = claudeCodeSessionId

    fun clearClaudeSession() {
        claudeCodeSessionId = null
    }

    private fun getDefaultWorkingDirectory(): String {
        // Use a generic home indicator that the CLI will resolve
        return "~"
    }

    fun getWelcomeMessage(): String = when (_cliMode.value) {
        CLIMode.CLAUDE_CODE -> {
            "🤖 Claude Code Session Ready\n" +
                "Type /claude or /cc followed by your command to execute with Claude Code.\n" +
                "Examples:\n" +
                "• /cc list files in current directory\n" +
                "• /cc create a new Python script\n" +
                "• /cc check git status"
        }
        CLIMode.CUE_CLI -> {
            "💬 Cue CLI Chat Ready\n" +
                "Start chatting with AI assistance and full context.\n" +
                "Examples:\n" +
                "• What files are in this directory?\n" +
                "• Help me write a Python script\n" +
                "• Explain this codebase structure"
        }
    }
}
