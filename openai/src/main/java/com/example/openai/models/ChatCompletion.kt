package com.example.openai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletion(
    val id: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val `object`: String,
    @SerialName("system_fingerprint")
    val systemFingerprint: String
)

@Serializable
data class Choice(
    val index: Int,
    val message: AssistantMessage,
    @SerialName("finish_reason")
    val finishReason: String
)

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 1000,
    val temperature: Double = 1.0,
    val tools: List<Tool>? = null,
    @SerialName("tool_choice")
    val toolChoice: String? = null
)