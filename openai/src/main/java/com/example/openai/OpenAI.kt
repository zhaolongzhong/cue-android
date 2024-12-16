package com.example.openai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "OpenAI"

class OpenAI(
    configuration: Configuration,
    client: OpenAIClient = OpenAIClient(configuration)
) {
    // Configuration
    data class Configuration(
        val apiKey: String,
        val baseUrl: String = "https://api.openai.com/v1"
    )

    // Error types
    sealed class Error : Exception() {
        data class NetworkError(val cause: Throwable) : Error()
        data class ApiError(val message: String) : Error()
        data class DecodingError(val cause: Throwable) : Error()
        object InvalidResponse : Error()
    }

    // Public API interfaces
    val chat = ChatAPI(client)
}

class ChatAPI internal constructor(
    private val client: OpenAIClient
) {
    val completions = CompletionsAPI(client)
}

class CompletionsAPI internal constructor(
    private val client: OpenAIClient
) {
    suspend fun create(
        model: String,
        messages: List<ChatMessage>,
        maxTokens: Int = 1000,
        temperature: Double = 1.0,
        tools: List<Tool>? = null,
        toolChoice: String? = null
    ): ChatCompletion = withContext(Dispatchers.IO) {
        try {
            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                maxTokens = maxTokens,
                temperature = temperature,
                tools = tools,
                toolChoice = toolChoice
            )

            client.send(
                endpoint = "chat/completions",
                method = "POST",
                body = request
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating completion", e)
            throw when (e) {
                is OpenAI.Error -> e
                else -> OpenAI.Error.NetworkError(e)
            }
        }
    }
}