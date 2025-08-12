package com.example.cue.anthropic

import javax.inject.Inject
import javax.inject.Singleton
import com.example.cue.utils.AppLog as Log

private const val TAG = "AnthropicChatService"

@Singleton
class AnthropicChatService @Inject constructor(
    private val anthropicClient: AnthropicClient,
    private val anthropicModel: AnthropicModel,
    private val anthropicChatServiceV2: AnthropicChatServiceV2,
) {
    private val useV2 = true // Toggle this to test V2
    suspend fun sendMessage(message: String): String = try {
        if (useV2) {
            Log.d(TAG, "Using AnthropicModelV2 implementation")
            anthropicChatServiceV2.sendMessage(message)
        } else {
            Log.d(TAG, "Using AnthropicModel SDK implementation (direct API)")
            anthropicModel.createCompletion(
                prompt = message,
                maxTokens = 1000,
                temperature = 0.7,
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send message", e)
        val errorMessage = when {
            e.message?.contains("404") == true ->
                "API endpoint not found. Check your configuration - SDK requires direct API access, legacy client works with backend servers."
            e.message?.contains("401") == true || e.message?.contains("403") == true ->
                "Authentication failed. Check your API key or access token."
            else -> e.message ?: "Failed to send message"
        }
        throw ChatError.MessageSendFailed(errorMessage)
    }

    suspend fun sendMessageWithSystemPrompt(
        message: String,
        systemPrompt: String? = null,
    ): String = try {
        Log.d(TAG, "Using AnthropicModel SDK implementation with system prompt")
        anthropicModel.createCompletion(
            prompt = message,
            maxTokens = 1000,
            temperature = 0.7,
            systemPrompt = systemPrompt,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send message with system prompt", e)
        throw ChatError.MessageSendFailed(e.message ?: "Failed to send message")
    }

    suspend fun sendConversation(
        messages: List<AnthropicModel.ConversationMessage>,
        systemPrompt: String? = null,
    ): String = try {
        anthropicModel.createConversation(
            messages = messages,
            maxTokens = 1000,
            temperature = 0.7,
            systemPrompt = systemPrompt,
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to send conversation", e)
        throw ChatError.MessageSendFailed(e.message ?: "Failed to send conversation")
    }
}

sealed class ChatError : Exception() {
    data class MessageSendFailed(override val message: String) : ChatError()
}
