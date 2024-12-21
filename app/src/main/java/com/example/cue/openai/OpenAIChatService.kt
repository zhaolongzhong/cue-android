package com.example.cue.openai

import android.util.Log
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OpenAIChatService"

@Singleton
class OpenAIChatService @Inject constructor(
    private val networkClient: NetworkClient,
) {
    suspend fun sendMessage(message: String): String {
        return try {
            val response = networkClient.post<OpenAIChatResponse>(
                endpoint = "/assistant/chat",
                body = mapOf(
                    "message" to message,
                ),
                responseType = OpenAIChatResponse::class.java,
            )
            response.message
        } catch (e: NetworkError) {
            Log.e(TAG, "Failed to send message", e)
            throw ChatError.MessageSendFailed(e.message ?: "Failed to send message")
        }
    }
}

sealed class ChatError : Exception() {
    data class MessageSendFailed(override val message: String) : ChatError()
}

data class OpenAIChatResponse(
    val message: String,
)