package ai.plusonelabs.anthropic

import ai.plusonelabs.agent.Agent
import ai.plusonelabs.agent.Session
import ai.plusonelabs.agent.models.SimpleChatMessage
import ai.plusonelabs.agent.models.SimpleContentBlock
import ai.plusonelabs.auth.TokenManager
import ai.plusonelabs.settings.apikeys.ApiKeyType
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import ai.plusonelabs.utils.AppLog as Log

private const val TAG = "AnthropicChatServiceV2"

@Singleton
class AnthropicChatServiceV2 @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    @Named("baseUrl") private val baseUrl: String,
    private val tokenManager: TokenManager,
) {
    private val anthropicModelV2 = AnthropicModelV2(
        apiKeyProvider = { getApiKey() },
        sharedPreferences = sharedPreferences,
        baseUrl = baseUrl.removeSuffix("/api/v1"),
        authTokenProvider = { tokenManager.getAccessToken() },
    )

    private val session = Session<SimpleChatMessage>(UUID.randomUUID().toString())

    private val agent = Agent(
        model = "claude-3-5-haiku-20241022",
        systemPrompt = "You are a helpful assistant.",
        maxTokens = 1000,
    )

    private fun getApiKey(): String? {
        // Use the same key retrieval logic as the original AnthropicModel
        val userApiKey = sharedPreferences.getString(ApiKeyType.ANTHROPIC.key, "")
        return if (!userApiKey.isNullOrEmpty()) {
            Log.d(TAG, "Using Anthropic API key from user settings")
            userApiKey
        } else {
            Log.d(TAG, "No user API key found")
            null
        }
    }

    suspend fun sendMessage(message: String): String = try {
        Log.d(TAG, "Using AnthropicModelV2 streaming for message: $message")

        // Add user message to session
        val userMessage = SimpleChatMessage.user(message)
        session.addMessage(userMessage)

        // Use streaming by default for better UX
        val responseBuilder = StringBuilder()
        var completeMessage: com.anthropic.models.messages.Message? = null
        var hasError = false

        try {
            anthropicModelV2.streamResponse(agent, session).collect { event ->
                when (event) {
                    is StreamEvent.ContentDelta -> {
                        responseBuilder.append(event.text)
                        // In a real implementation, you'd emit these chunks for real-time display
                    }
                    is StreamEvent.MessageComplete -> {
                        completeMessage = event.message
                        Log.d(TAG, "Stream completed with message id: ${event.message.id()}")
                    }
                    is StreamEvent.MessageCompleteWithText -> {
                        Log.d(TAG, "Stream completed with text (${event.text.length} chars), message id: ${event.messageId}")
                        // Text is already in responseBuilder from ContentDelta events
                    }
                    is StreamEvent.Error -> {
                        throw event.exception
                    }
                }
            }
        } catch (e: Exception) {
            // If streaming fails but we have collected text, use it
            if (responseBuilder.isNotEmpty()) {
                Log.w(TAG, "Stream failed but recovered text: ${e.message}")
                hasError = true
            } else {
                throw e
            }
        }

        val responseString = if (responseBuilder.isNotEmpty()) {
            responseBuilder.toString()
        } else {
            "No response content found"
        }

        // Add assistant message to session
        val assistantMessage = SimpleChatMessage.assistant(responseString)
        session.addMessage(assistantMessage)

        Log.d(TAG, "Response received: $responseString")
        responseString
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send message using AnthropicModelV2", e)
        val errorMessage = when {
            e.message?.contains("404") == true ->
                "API endpoint not found. Check your configuration."
            e.message?.contains("401") == true || e.message?.contains("403") == true ->
                "Authentication failed. Check your API key."
            else -> e.message ?: "Failed to send message"
        }
        throw ChatError.MessageSendFailed(errorMessage)
    }

    fun sendMessageStream(message: String): Flow<String> {
        Log.d(TAG, "Using AnthropicModelV2 streaming for message: $message")

        // Add user message to session
        val userMessage = SimpleChatMessage.user(message)
        session.addMessage(userMessage)

        return anthropicModelV2.streamResponse(agent, session).map { event ->
            when (event) {
                is StreamEvent.ContentDelta -> event.text
                is StreamEvent.MessageComplete -> {
                    Log.d(TAG, "Stream completed with message id: ${event.message.id()}")

                    // Add the complete message to session for context
                    val responseText = StringBuilder()
                    event.message.content().forEach { contentBlock ->
                        contentBlock.text().ifPresent { textBlock ->
                            responseText.append(textBlock.text())
                        }
                    }
                    val assistantMessage = SimpleChatMessage.assistant(responseText.toString())
                    session.addMessage(assistantMessage)

                    "" // Empty string to indicate completion
                }
                is StreamEvent.MessageCompleteWithText -> {
                    Log.d(TAG, "Stream completed with text (${event.text.length} chars), message id: ${event.messageId}")

                    // Add the complete message to session for context
                    val assistantMessage = SimpleChatMessage.assistant(event.text)
                    session.addMessage(assistantMessage)

                    "" // Empty string to indicate completion
                }
                is StreamEvent.Error -> {
                    Log.e(TAG, "Stream error: ${event.exception.message}")
                    throw event.exception
                }
            }
        }
    }

    fun clearSession() {
        session.clear()
        Log.d(TAG, "Session cleared")
    }

    fun getSessionMessageCount(): Int = session.size()
}
