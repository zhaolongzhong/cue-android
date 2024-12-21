package com.example.cue.openai

import android.util.Log
import com.example.cue.BuildConfig
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "OpenAIChatService"

@Singleton
class OpenAIChatService @Inject constructor(
    @Named("openaiClient") private val networkClient: NetworkClient,
) {
    private val chatMessages = mutableListOf<ChatMessage>()

    suspend fun sendMessage(message: String): String {
        // Add user message to chat history
        chatMessages.add(ChatMessage(role = "user", content = message))

        return try {
            val response = networkClient.post<OpenAIResponse>(
                endpoint = "/chat/completions",
                body = mapOf(
                    "model" to "gpt-3.5-turbo",
                    "messages" to chatMessages.map { 
                        mapOf(
                            "role" to it.role,
                            "content" to it.content
                        )
                    },
                    "temperature" to 0.7,
                    "max_tokens" to 1000,
                ),
                responseType = OpenAIResponse::class.java,
            )
            val assistantMessage = response.choices.firstOrNull()?.message
                ?: throw ChatError.MessageSendFailed("No response from OpenAI")

            // Add assistant's response to chat history
            chatMessages.add(assistantMessage)

            assistantMessage.content
        } catch (e: NetworkError) {
            Log.e(TAG, "Failed to send message", e)
            throw ChatError.MessageSendFailed(e.message ?: "Failed to send message")
        }
    }

    fun clearChat() {
        chatMessages.clear()
    }
}

sealed class ChatError : Exception() {
    data class MessageSendFailed(override val message: String) : ChatError()
}

data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>,
)

data class Choice(
    val message: ChatMessage,
)

data class ChatMessage(
    val role: String,
    val content: String,
)