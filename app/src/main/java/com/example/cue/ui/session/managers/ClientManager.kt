package com.example.cue.ui.session.managers

import com.example.cue.ui.session.CLIClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import com.example.cue.utils.AppLog as Log

class ClientManager {
    companion object {
        private const val TAG = "ClientManager"
    }

    private val _availableClients = MutableStateFlow<List<CLIClient>>(emptyList())
    val availableClients: StateFlow<List<CLIClient>> = _availableClients.asStateFlow()

    private val _selectedClientId = MutableStateFlow<String?>(null)
    val selectedClientId: StateFlow<String?> = _selectedClientId.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    fun addOrUpdateClient(
        clientId: String,
        shortName: String? = null,
        platform: String? = null,
        hostname: String? = null,
        cwd: String? = null,
    ): CLIClient? {
        // Skip desktop clients - we only want CLI clients
        if (clientId.startsWith("desktop-")) {
            Log.d(TAG, "Skipping desktop client: $clientId")
            return null
        }

        // Extract info from clientId pattern: cli-{timestamp}-{id}-{platform}
        val parts = clientId.split("-")
        val derivedPlatform = if (clientId.startsWith("cli-")) {
            "cli"
        } else {
            platform ?: "unknown"
        }

        val derivedShortName = shortName
            ?: if (parts.size >= 3) "${parts[0]}-${parts[2]}" else clientId.take(12)

        val derivedHostname = hostname
            ?: if (parts.size >= 4) parts[3] else null

        val client = CLIClient(
            clientId = clientId,
            shortName = derivedShortName,
            displayName = derivedShortName,
            platform = derivedPlatform.uppercase(),
            hostname = derivedHostname,
            cwd = cwd,
            isOnline = true,
            connectedAt = Date(),
        )

        val updatedClients = _availableClients.value.toMutableList()
        updatedClients.removeAll { it.clientId == clientId }
        updatedClients.add(client)
        _availableClients.value = updatedClients

        Log.d(TAG, "Added/Updated client: $derivedShortName ($derivedPlatform) - ID: $clientId, Total clients: ${_availableClients.value.size}")
        return client
    }

    fun removeClient(clientId: String) {
        val updatedClients = _availableClients.value.toMutableList()
        updatedClients.removeAll { it.clientId == clientId }
        _availableClients.value = updatedClients

        // Clear selection if disconnected client was selected
        if (_selectedClientId.value == clientId) {
            _selectedClientId.value = null
        }

        Log.d(TAG, "Removed client: $clientId, Remaining clients: ${_availableClients.value.size}")
    }

    fun selectClient(clientId: String) {
        _selectedClientId.value = clientId
        Log.d(TAG, "Selected client: $clientId")
    }

    fun clearSelection() {
        _selectedClientId.value = null
    }

    fun startDiscovery() {
        _isDiscovering.value = true
        _availableClients.value = emptyList()
        Log.d(TAG, "Started client discovery")
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        Log.d(TAG, "Stopped client discovery, found ${_availableClients.value.size} clients")
    }

    fun clearAll() {
        _availableClients.value = emptyList()
        _selectedClientId.value = null
    }

    fun getSelectedClient(): CLIClient? = _availableClients.value.find { it.clientId == _selectedClientId.value }

    fun shouldAutoSelectClient(clientId: String): Boolean {
        val client = _availableClients.value.find { it.clientId == clientId }
        return _selectedClientId.value == null && client?.platform?.lowercase() == "cli"
    }
}
