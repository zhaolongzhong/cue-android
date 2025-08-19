package com.example.cue.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.Environment
import com.example.cue.network.websocket.EventMessage
import com.example.cue.network.websocket.WebSocketService
import com.example.cue.ui.session.handlers.AgentControlEventHandler
import com.example.cue.ui.session.handlers.ClientEventHandler
import com.example.cue.ui.session.handlers.HandlerContext
import com.example.cue.ui.session.handlers.SessionEventHandler
import com.example.cue.ui.session.handlers.TaskStatusEventHandler
import com.example.cue.ui.session.handlers.WebSocketEventHandler
import com.example.cue.ui.session.managers.ClientManager
import com.example.cue.ui.session.managers.MessageManager
import com.example.cue.ui.session.managers.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.cue.utils.AppLog as Log

@HiltViewModel
class SessionChatViewModel@Inject constructor(
    private val webSocketService: WebSocketService,
    private val environment: Environment,
) : ViewModel() {

    companion object {
        private const val TAG = "SessionChatViewModel"
    }

    // Managers
    private val clientManager = ClientManager()
    private val messageManager = MessageManager()
    private val sessionManager = SessionManager()

    // Event Handlers
    private val eventHandlers = listOf(
        ClientEventHandler(),
        SessionEventHandler(),
        AgentControlEventHandler(),
        TaskStatusEventHandler(),
    )

    // Public state flows
    val messages: StateFlow<List<CLIMessage>> = messageManager.messages
    val isLoading: StateFlow<Boolean> = sessionManager.isLoading
    val currentSession: StateFlow<CLISession?> = sessionManager.currentSession
    val cliMode: StateFlow<CLIMode> = sessionManager.cliMode
    val availableClients: StateFlow<List<CLIClient>> = clientManager.availableClients
    val selectedClientId: StateFlow<String?> = clientManager.selectedClientId
    val isDiscovering: StateFlow<Boolean> = clientManager.isDiscovering

    // Connection status as String for UI compatibility
    private val _connectionStatus = kotlinx.coroutines.flow.MutableStateFlow("disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus

    init {
        createNewSession()
        observeWebSocketEvents()
        observeConnectionState()
    }

    fun initializeSession(session: CLISession) {
        Log.d(TAG, "Initializing session: ${session.id}")
        // TODO: Implement session initialization logic
        messageManager.clearMessages()
        messageManager.addStatusMessage(
            content = "Session initialized: ${session.displayName}",
            sessionId = session.id,
        )
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                event?.let { handleWebSocketEvent(it) }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketService.connectionState.collect { state ->
                _connectionStatus.value = when (state) {
                    is com.example.cue.network.websocket.ConnectionState.Connected -> "connected"
                    is com.example.cue.network.websocket.ConnectionState.Connecting -> "connecting"
                    is com.example.cue.network.websocket.ConnectionState.Disconnected -> "disconnected"
                    is com.example.cue.network.websocket.ConnectionState.Error -> "error"
                }
            }
        }
    }

    private suspend fun handleWebSocketEvent(event: EventMessage) {
        val context = HandlerContext(
            currentSessionId = currentSession.value?.id,
            claudeCodeSessionId = sessionManager.getClaudeSessionId(),
            clientId = environment.clientId,
            onSessionCreated = { /* Handle if needed */ },
            onSessionDestroyed = { sessionManager.clearClaudeSession() },
            onClaudeSessionUpdate = { sessionManager.updateClaudeSessionId(it) },
            onLoadingChanged = { isLoading -> sessionManager.setLoading(isLoading) },
        )

        eventHandlers.forEach { handler ->
            if (handler.canHandle(event)) {
                handler.handle(event, clientManager, messageManager, context)
                return
            }
        }

        Log.d(TAG, "No handler found for event type: ${event.type}")
    }

    fun createNewSession() {
        val selectedClient = clientManager.getSelectedClient()
        val session = sessionManager.createNewSession(
            selectedClientId = selectedClient?.clientId,
            selectedClientName = selectedClient?.displayName,
        )

        messageManager.clearMessages()
        messageManager.addStatusMessage(
            content = sessionManager.getWelcomeMessage(),
            sessionId = session.id,
        )

        // Send session creation request if we have a selected client
        selectedClient?.clientId?.let {
            webSocketService.createSession(session.id, session.cwd ?: "~")
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val session = currentSession.value ?: return
        Log.d(TAG, "sendMessage called with: '$text'")

        // Add user message
        messageManager.addCommandMessage(text, session.id)

        // Handle special commands
        when {
            text == "/mode" -> toggleMode()
            text == "/new" -> createNewSession()
            text == "/clear" -> clearChat()
            text == "/discover" -> refreshClients()
            text.startsWith("/claude ") || text.startsWith("/cc ") -> {
                val prompt = text
                    .replace("/claude ", "")
                    .replace("/cc ", "")
                    .trim()
                sendClaudeCodeRequest(prompt)
            }
            else -> {
                // Handle based on mode
                when (cliMode.value) {
                    CLIMode.CLAUDE_CODE -> sendClaudeCodeRequest(text)
                    CLIMode.CUE_CLI -> sendCueCLIRequest(text)
                }
            }
        }
    }

    private fun sendClaudeCodeRequest(prompt: String) {
        val session = currentSession.value ?: return
        Log.d(TAG, "sendClaudeCodeRequest called with prompt: '$prompt'")

        sessionManager.setLoading(true)
        messageManager.addStatusMessage(
            content = "⚙️ Executing Claude Code...",
            status = CLIStatus.EXECUTING,
            sessionId = session.id,
        )

        val requestId = webSocketService.sendClaudeCodeRequest(
            prompt = prompt,
            sessionId = sessionManager.getClaudeSessionId(),
            workingDirectory = session.cwd ?: "~",
        )

        // Add to pending requests if using AgentControlEventHandler
        (eventHandlers.find { it is AgentControlEventHandler } as? AgentControlEventHandler)
            ?.addPendingRequest(requestId)

        Log.d(TAG, "Claude Code request sent - prompt: '$prompt', requestId: $requestId")
    }

    private fun sendCueCLIRequest(prompt: String) {
        val session = currentSession.value ?: return
        Log.d(TAG, "sendCueCLIRequest called with prompt: '$prompt'")

        sessionManager.setLoading(true)
        messageManager.addStatusMessage(
            content = "💬 Processing with Cue CLI...",
            status = CLIStatus.EXECUTING,
            sessionId = session.id,
        )

        val selectedClient = clientManager.getSelectedClient()

        // Get existing session ID - throw error if none exists
        val sessionIdToUse = sessionManager.getClaudeSessionId() ?: session.id
        if (sessionIdToUse.isBlank()) {
            val errorMsg = "No valid session ID available for Cue CLI request"
            Log.e(TAG, errorMsg)
            messageManager.addErrorMessage(
                content = errorMsg,
                sessionId = session.id,
            )
            sessionManager.setLoading(false)
            return
        }

        val requestId = webSocketService.sendCueCLIRequest(
            message = prompt,
            sessionId = sessionIdToUse,
            workingDirectory = session.cwd ?: "~",
            targetClientId = selectedClient?.clientId,
        )

        // Add to pending requests if using AgentControlEventHandler
        (eventHandlers.find { it is AgentControlEventHandler } as? AgentControlEventHandler)
            ?.addPendingRequest(requestId)

        Log.d(TAG, "Cue CLI request sent - prompt: '$prompt', requestId: $requestId")
    }

    fun toggleMode() {
        val newMode = sessionManager.toggleMode()
        val modeName = when (newMode) {
            CLIMode.CLAUDE_CODE -> "Claude Code"
            CLIMode.CUE_CLI -> "Cue CLI"
        }

        messageManager.addStatusMessage(
            content = "Switched to $modeName mode",
            sessionId = currentSession.value?.id,
        )
    }

    fun selectClient(clientId: String) {
        clientManager.selectClient(clientId)
        val client = clientManager.getSelectedClient()
        client?.let {
            messageManager.addStatusMessage(
                content = "Selected client: ${it.displayName}",
                sessionId = currentSession.value?.id,
            )
        }
    }

    fun refreshClients() {
        clientManager.startDiscovery()
        webSocketService.requestClientsList()

        messageManager.addStatusMessage(
            content = "🔄 Discovering available CLI clients...",
            sessionId = currentSession.value?.id,
        )

        // Stop discovering after 5 seconds
        viewModelScope.launch {
            delay(5000)
            clientManager.stopDiscovery()
        }
    }

    private fun clearChat() {
        messageManager.clearMessages()
        messageManager.addStatusMessage(
            content = "Chat cleared",
            sessionId = currentSession.value?.id,
        )
    }

    fun connectWebSocket() {
        webSocketService.connect()
    }

    fun disconnectWebSocket() {
        webSocketService.disconnect()
    }
}
