package com.example.cue.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cue.network.websocket.ConnectionState
import com.example.cue.network.websocket.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import com.example.cue.utils.AppLog as Log

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val webSocketService: WebSocketService,
) : ViewModel() {

    companion object {
        private const val TAG = "SessionListViewModel"
    }

    private val _sessions = MutableStateFlow<List<CLISession>>(emptyList())
    val sessions: StateFlow<List<CLISession>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectionStatus = MutableStateFlow("disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _managerClient = MutableStateFlow<ManagerClient?>(null)
    val managerClient: StateFlow<ManagerClient?> = _managerClient.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _availableClients = MutableStateFlow<List<CLIClient>>(emptyList())
    val availableClients: StateFlow<List<CLIClient>> = _availableClients.asStateFlow()

    private val _starredSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val starredSessionIds: StateFlow<Set<String>> = _starredSessionIds.asStateFlow()

    init {
        observeConnectionStatus()
        observeManagerClient()
        observeWebSocketEvents()
        loadSessions()
        loadStarredSessions()
    }

    fun startDiscovery() {
        Log.d(TAG, "startDiscovery called")
        viewModelScope.launch {
            _isDiscovering.value = true
            try {
                Log.d(TAG, "Connecting to WebSocket service...")
                webSocketService.connect()

                // Send request_clients event to discover available CLI clients
                Log.d(TAG, "Sending request_clients event to WebSocket")
                val requestId = webSocketService.requestClientsList()
                Log.i(TAG, "Sent request_clients with requestId: $requestId")

                // TODO: Listen for client_info events in response
                // For now, keep empty list until we receive real clients
                _availableClients.value = emptyList()
                Log.d(TAG, "Waiting for client_info events from WebSocket...")
            } catch (e: Exception) {
                Log.e(TAG, "ERROR in startDiscovery: ${e.message}", e)
                // Handle error
            } finally {
                // Keep discovering for a bit to receive responses
                kotlinx.coroutines.delay(2000) // Wait 2 seconds for responses
                _isDiscovering.value = false
                Log.d(TAG, "Discovery completed, found ${_availableClients.value.size} clients")
            }
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch {
            webSocketService.disconnect()
        }
    }

    fun createSession(
        workingDirectory: String = "/Users",
        isClaudeCodeMode: Boolean = false,
    ): CLISession? {
        val client = _managerClient.value ?: return null

        val session = CLISession(
            id = UUID.randomUUID().toString(),
            displayName = "CLI Session ${(_sessions.value.size + 1)}",
            cwd = workingDirectory,
            createdAt = Date(),
            isActive = true,
            targetClientId = client.clientId,
            targetClientName = client.displayName,
            sessionInfo = SessionInfo(
                sessionId = UUID.randomUUID().toString(),
                clientId = client.clientId,
                messageCount = 0,
            ),
        )

        _sessions.value = _sessions.value + session
        Log.i(TAG, "Created new session: ${session.displayName}")
        // TODO: Persist new session to storage/database
        return session
    }

    fun removeSession(session: CLISession) {
        Log.d(TAG, "removeSession called for session: ${session.id} - ${session.displayName}")
        val previousSize = _sessions.value.size
        _sessions.value = _sessions.value.filter { it.id != session.id }
        val newSize = _sessions.value.size
        Log.d(TAG, "Sessions updated: $previousSize -> $newSize sessions")

        if (previousSize == newSize) {
            Log.e(TAG, "ERROR: Failed to remove session ${session.id}. Session not found in list!")
        } else {
            Log.i(TAG, "Successfully removed session: ${session.displayName}")
            // Remove from starred if it was starred
            if (_starredSessionIds.value.contains(session.id)) {
                _starredSessionIds.value = _starredSessionIds.value - session.id
                saveStarredSessions()
            }
            // TODO: Persist session deletion to storage/database
        }
    }

    fun toggleSessionStar(session: CLISession) {
        Log.d(TAG, "Toggling star for session: ${session.id}")
        val currentStarred = _starredSessionIds.value
        _starredSessionIds.value = if (currentStarred.contains(session.id)) {
            Log.i(TAG, "Unstarring session: ${session.displayName}")
            currentStarred - session.id
        } else {
            Log.i(TAG, "Starring session: ${session.displayName}")
            currentStarred + session.id
        }
        saveStarredSessions()
    }

    fun isSessionStarred(sessionId: String): Boolean = _starredSessionIds.value.contains(sessionId)

    fun renameSession(session: CLISession, newName: String) {
        Log.d(TAG, "Renaming session ${session.id} to: $newName")
        _sessions.value = _sessions.value.map {
            if (it.id == session.id) {
                it.copy(displayName = newName, customTitle = newName)
            } else {
                it
            }
        }
        // TODO: Persist renamed session to storage/database
        Log.i(TAG, "Session renamed successfully")
    }

    fun archiveSession(session: CLISession) {
        Log.d(TAG, "Archiving session: ${session.id}")
        _sessions.value = _sessions.value.map {
            if (it.id == session.id) {
                it.copy(isArchived = true)
            } else {
                it
            }
        }
        // TODO: Persist archived session to storage/database
        Log.i(TAG, "Session archived successfully")
    }

    fun refreshClients() {
        viewModelScope.launch {
            // TODO: Implement actual client refresh
            // For now, just simulate a refresh
            _managerClient.value = _managerClient.value
        }
    }

    private fun observeConnectionStatus() {
        viewModelScope.launch {
            webSocketService.connectionState.collect { state ->
                val previousStatus = _connectionStatus.value
                _connectionStatus.value = when (state) {
                    is ConnectionState.Connected -> {
                        Log.i(TAG, "WebSocket connected successfully")
                        "connected"
                    }
                    is ConnectionState.Connecting -> {
                        Log.d(TAG, "WebSocket connecting...")
                        "connecting"
                    }
                    is ConnectionState.Disconnected -> {
                        Log.w(TAG, "WebSocket disconnected")
                        "disconnected"
                    }
                    is ConnectionState.Error -> {
                        Log.e(TAG, "WebSocket error occurred")
                        "error"
                    }
                }

                if (previousStatus != _connectionStatus.value) {
                    Log.d(TAG, "Connection status changed: $previousStatus -> ${_connectionStatus.value}")
                }
            }
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketService.events.collect { event ->
                when (event?.type) {
                    com.example.cue.network.websocket.EventMessageType.PING -> {
                        // Handle ping messages that contain client_info
                        val payloadMap = event.payload as? Map<*, *>
                        val payloadType = payloadMap?.get("type") as? String

                        if (payloadType == "client_info") {
                            Log.d(TAG, "Received client_info via PING event from ${event.clientId}")
                            Log.d(TAG, "client_info payload contents: $payloadMap")
                            val clientId = event.clientId
                            if (clientId != null && clientId != webSocketService.getClientId()) {
                                try {
                                    // The clientInfo can be in two places:
                                    // 1. In the payload.clientInfo (for some messages)
                                    // 2. At the root level event.clientInfo (for backend messages)
                                    var clientInfo = payloadMap["clientInfo"] as? Map<*, *>

                                    // If not in payload, check root level
                                    if (clientInfo == null && event.clientInfo != null) {
                                        clientInfo = event.clientInfo as? Map<*, *>
                                        Log.d(TAG, "Using clientInfo from root level: $clientInfo")
                                    } else {
                                        Log.d(TAG, "clientInfo from payload: $clientInfo")
                                    }

                                    if (clientInfo != null) {
                                        val shortName = clientInfo["shortName"] as? String
                                        val cwd = clientInfo["cwd"] as? String

                                        // Platform is nested in metadata.system
                                        val metadata = clientInfo["metadata"] as? Map<*, *>
                                        val system = metadata?.get("system") as? Map<*, *>
                                        val platform = system?.get("platform") as? String ?: "unknown"
                                        val hostname = system?.get("hostname") as? String

                                        Log.d(TAG, "Parsed values - platform: $platform, shortName: $shortName, hostname: $hostname, cwd: $cwd")

                                        val newClient = CLIClient(
                                            clientId = clientId,
                                            shortName = shortName ?: clientId.take(8),
                                            displayName = shortName ?: "CLI Client",
                                            platform = platform,
                                            hostname = hostname,
                                            cwd = cwd,
                                            isOnline = true,
                                        )

                                        // Add to available clients if not already present
                                        val currentClients = _availableClients.value.toMutableList()
                                        if (currentClients.none { it.clientId == clientId }) {
                                            currentClients.add(newClient)
                                            _availableClients.value = currentClients
                                            Log.i(TAG, "Added new client from ping: ${newClient.displayName} (ID: $clientId, platform: $platform)")
                                        } else {
                                            Log.d(TAG, "Client already in list: $clientId")
                                        }
                                    } else {
                                        Log.w(TAG, "No clientInfo object in client_info response from $clientId")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing client_info from PING: ${e.message}")
                                }
                            }
                        }
                    }
                    com.example.cue.network.websocket.EventMessageType.CLIENT_STATUS -> {
                        Log.d(TAG, "Received CLIENT_STATUS event")
                        // CLIENT_STATUS events contain client info when responding to request_clients
                        // Parse the payload based on its actual structure
                        val payloadMap = event.payload as? Map<*, *>
                        if (payloadMap != null) {
                            Log.d(TAG, "CLIENT_STATUS payload: $payloadMap")
                            try {
                                val clientId = event.clientId ?: payloadMap["client_id"] as? String
                                if (clientId != null && clientId != webSocketService.getClientId()) {
                                    val clientName = payloadMap["client_name"] as? String
                                    val platform = payloadMap["platform"] as? String ?: "unknown"
                                    val hostname = payloadMap["hostname"] as? String
                                    val cwd = payloadMap["cwd"] as? String

                                    val newClient = CLIClient(
                                        clientId = clientId,
                                        shortName = clientName?.take(10) ?: clientId.take(8),
                                        displayName = clientName ?: "CLI Client",
                                        platform = platform,
                                        hostname = hostname,
                                        cwd = cwd,
                                        isOnline = true,
                                    )

                                    // Add to available clients if not already present
                                    val currentClients = _availableClients.value.toMutableList()
                                    if (currentClients.none { it.clientId == clientId }) {
                                        currentClients.add(newClient)
                                        _availableClients.value = currentClients
                                        Log.i(TAG, "Added new client from CLIENT_STATUS: $clientName (ID: $clientId)")
                                    } else {
                                        Log.d(TAG, "Client already in list: $clientId")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing CLIENT_STATUS payload: ${e.message}")
                            }
                        }
                    }
                    com.example.cue.network.websocket.EventMessageType.CLIENT_DISCONNECT -> {
                        Log.d(TAG, "Received CLIENT_DISCONNECT event")
                        val payloadMap = event.payload as? Map<*, *>
                        val clientId = event.clientId ?: payloadMap?.get("client_id") as? String
                        if (clientId != null) {
                            _availableClients.value = _availableClients.value.filter { it.clientId != clientId }
                            Log.i(TAG, "Removed disconnected client: $clientId")
                        }
                    }
                    else -> {
                        // Ignore other event types
                    }
                }
            }
        }
    }

    private fun observeManagerClient() {
        // TODO: Implement actual manager client observation from WebSocket
        // NO MOCK DATA - manager client should come from real WebSocket connection
        viewModelScope.launch {
            webSocketService.connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    // TODO: Get real manager client from WebSocket
                    // For now, keep null until real implementation
                    _managerClient.value = null
                    Log.d(TAG, "WebSocket connected, waiting for real manager client")
                } else {
                    _managerClient.value = null
                }
            }
        }
    }

    private fun loadSessions() {
        // TODO: Load persisted sessions from storage/database
        // Start with empty list - NO MOCK DATA
        Log.i(TAG, "Loading sessions - starting with empty list, no mock data")
    }

    private fun loadStarredSessions() {
        // TODO: Load starred sessions from SharedPreferences or database
        // For now, start with empty set
        Log.i(TAG, "Loading starred sessions - starting with empty set")
        _starredSessionIds.value = emptySet()
    }

    private fun saveStarredSessions() {
        // TODO: Save starred sessions to SharedPreferences or database
        Log.i(TAG, "Saving starred sessions: ${_starredSessionIds.value.size} sessions starred")
    }
}

// Data class for manager client
data class ManagerClient(
    val clientId: String,
    val clientName: String?,
    val userAgent: String?,
    val isOnline: Boolean,
    val displayName: String,
)
