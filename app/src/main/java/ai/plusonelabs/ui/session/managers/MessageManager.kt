package ai.plusonelabs.ui.session.managers

import ai.plusonelabs.ui.session.CLIMessage
import ai.plusonelabs.ui.session.CLIMessageType
import ai.plusonelabs.ui.session.CLISession
import ai.plusonelabs.ui.session.CLIStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import ai.plusonelabs.utils.AppLog as Log

class MessageManager {
    companion object {
        private const val TAG = "MessageManager"
    }

    private val _messages = MutableStateFlow<List<CLIMessage>>(emptyList())
    val messages: StateFlow<List<CLIMessage>> = _messages.asStateFlow()

    fun addMessage(message: CLIMessage) {
        _messages.value = _messages.value + message
        Log.d(TAG, "Added message: ${message.type} - ${message.content.take(50)}")
    }

    fun addStatusMessage(content: String, status: CLIStatus? = null, sessionId: String? = null) {
        addMessage(
            CLIMessage(
                type = CLIMessageType.STATUS,
                content = content,
                status = status,
                sessionId = sessionId,
            ),
        )
    }

    fun addCommandMessage(content: String, sessionId: String? = null) {
        addMessage(
            CLIMessage(
                type = CLIMessageType.COMMAND,
                content = content,
                sessionId = sessionId,
            ),
        )
    }

    fun addResponseMessage(content: String, sessionId: String? = null) {
        addMessage(
            CLIMessage(
                type = CLIMessageType.RESPONSE,
                content = content,
                sessionId = sessionId,
            ),
        )
    }

    fun addErrorMessage(content: String, sessionId: String? = null) {
        addMessage(
            CLIMessage(
                type = CLIMessageType.ERROR,
                content = content,
                status = CLIStatus.ERROR,
                sessionId = sessionId,
            ),
        )
    }

    fun updateLastExecutingMessage(content: String, status: CLIStatus) {
        val messages = _messages.value.toMutableList()
        val lastStatusIndex = messages.indexOfLast {
            it.type == CLIMessageType.STATUS &&
                it.status == CLIStatus.EXECUTING
        }

        if (lastStatusIndex != -1) {
            val existingMessage = messages[lastStatusIndex]
            val statusIcon = when (status) {
                CLIStatus.RECEIVED -> "📥"
                CLIStatus.VALIDATED -> "✅"
                CLIStatus.EXECUTING -> "⚙️"
                CLIStatus.COMPLETED -> "✔️"
                CLIStatus.ERROR -> "❌"
            }

            val formattedContent = "$statusIcon $content"
            val newContent = if (status == CLIStatus.COMPLETED || existingMessage.content.contains("\n")) {
                "${existingMessage.content}\n$formattedContent"
            } else {
                formattedContent
            }

            messages[lastStatusIndex] = existingMessage.copy(
                content = newContent,
                status = status,
            )
            _messages.value = messages
            Log.d(TAG, "Updated executing message with status: $status")
        }
    }

    fun updateOrCreateMessageByStreamId(messageId: String, content: String, status: CLIStatus, sessionId: String? = null) {
        val messages = _messages.value.toMutableList()
        val existingIndex = messages.indexOfFirst { it.id == messageId }

        if (existingIndex != -1) {
            // Update existing message
            val existingMessage = messages[existingIndex]
            val statusIcon = when (status) {
                CLIStatus.RECEIVED -> "📥"
                CLIStatus.VALIDATED -> "✅"
                CLIStatus.EXECUTING -> "⚙️"
                CLIStatus.COMPLETED -> "✔️"
                CLIStatus.ERROR -> "❌"
            }

            messages[existingIndex] = existingMessage.copy(
                content = "$statusIcon $content",
                status = status,
            )
            Log.d(TAG, "Updated streaming message $messageId with status: $status")
        } else {
            // Create new message with specific ID
            val statusIcon = when (status) {
                CLIStatus.RECEIVED -> "📥"
                CLIStatus.VALIDATED -> "✅"
                CLIStatus.EXECUTING -> "⚙️"
                CLIStatus.COMPLETED -> "✔️"
                CLIStatus.ERROR -> "❌"
            }

            val newMessage = CLIMessage(
                id = messageId,
                type = CLIMessageType.STATUS,
                content = "$statusIcon $content",
                status = status,
                sessionId = sessionId,
            )
            messages.add(newMessage)
            Log.d(TAG, "Created new streaming message $messageId with status: $status")
        }

        _messages.value = messages
    }

    fun clearMessages() {
        _messages.value = emptyList()
        Log.d(TAG, "Cleared all messages")
    }

    fun getMessagesForSession(sessionId: String): List<CLIMessage> = _messages.value.filter { it.sessionId == sessionId }
}
