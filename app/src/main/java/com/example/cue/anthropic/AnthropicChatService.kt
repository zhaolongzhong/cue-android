package com.example.cue.anthropic

import android.util.Log
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "AnthropicChatService"

@Singleton
class AnthropicChatService @Inject constructor(
    @Named("useAnthropicSdk") private val useAnthropicSdk: Boolean,
    private val anthropicClient: AnthropicClient,
    private val anthropicModel: AnthropicModel,
) {
    suspend fun sendMessage(message: String): String {
        return try {
            if (useAnthropicSdk) {
                Log.d(TAG, "Using AnthropicModel SDK implementation (direct API)")
                anthropicModel.createCompletion(
                    prompt = message,
                    maxTokens = 1000,
                    temperature = 0.7,
                )
            } else {
                Log.d(TAG, "Using AnthropicClient legacy implementation (backend server)")
                anthropicClient.createCompletion(
                    prompt = message,
                    maxTokens = 1000,
                    temperature = 0.7,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message using ${if (useAnthropicSdk) "SDK" else "legacy client"}", e)
            val errorMessage = when {
                e.message?.contains("404") == true ->
                    "API endpoint not found. Check your configuration - SDK requires direct API access, legacy client works with backend servers."
                e.message?.contains("401") == true || e.message?.contains("403") == true ->
                    "Authentication failed. Check your API key or access token."
                else -> e.message ?: "Failed to send message"
            }
            throw ChatError.MessageSendFailed(errorMessage)
        }
    }

    suspend fun sendMessageWithSystemPrompt(
        message: String,
        systemPrompt: String? = null,
    ): String {
        return try {
            if (useAnthropicSdk) {
                Log.d(TAG, "Using AnthropicModel SDK implementation with system prompt")
                anthropicModel.createCompletion(
                    prompt = message,
                    maxTokens = 1000,
                    temperature = 0.7,
                    systemPrompt = systemPrompt,
                )
            } else {
                Log.d(TAG, "Using AnthropicClient legacy implementation (system prompt not supported)")
                anthropicClient.createCompletion(
                    prompt = message,
                    maxTokens = 1000,
                    temperature = 0.7,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message with system prompt", e)
            throw ChatError.MessageSendFailed(e.message ?: "Failed to send message")
        }
    }

    suspend fun sendConversation(
        messages: List<AnthropicModel.ConversationMessage>,
        systemPrompt: String? = null,
    ): String {
        return try {
            if (useAnthropicSdk) {
                Log.d(TAG, "Using AnthropicModel SDK for conversation")
                anthropicModel.createConversation(
                    messages = messages,
                    maxTokens = 1000,
                    temperature = 0.7,
                    systemPrompt = systemPrompt,
                )
            } else {
                Log.d(TAG, "Using AnthropicClient legacy implementation (falling back to last message)")
                val lastMessage = messages.lastOrNull { it.role == "user" }?.content
                    ?: throw ChatError.MessageSendFailed("No user message found in conversation")
                anthropicClient.createCompletion(
                    prompt = lastMessage,
                    maxTokens = 1000,
                    temperature = 0.7,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send conversation", e)
            throw ChatError.MessageSendFailed(e.message ?: "Failed to send conversation")
        }
    }
}

sealed class ChatError : Exception() {
    data class MessageSendFailed(override val message: String) : ChatError()
}
