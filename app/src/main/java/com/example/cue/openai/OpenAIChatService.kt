package com.example.cue.openai

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenAIChatService"

@Singleton
class OpenAIChatService @Inject constructor(
    private val openAIClient: OpenAIClient,
) {
    suspend fun sendMessage(message: String): String {
        return try {
            openAIClient.createCompletion(
                prompt = message,
                maxTokens = 1000,
                temperature = 0.7,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            throw ChatError.MessageSendFailed(e.message ?: "Failed to send message")
        }
    }
}

sealed class ChatError : Exception() {
    data class MessageSendFailed(override val message: String) : ChatError()
}
