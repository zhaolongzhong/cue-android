package com.example.openai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface Message {
    val role: String
    val content: String?
}

@Serializable
data class ToolMessage(
    override val role: String = "tool",
    override val content: String,
    @SerialName("tool_call_id")
    val toolCallId: String
) : Message

@Serializable
data class UserMessage(
    override val role: String = "user",
    override val content: String
) : Message

@Serializable
data class AssistantMessage(
    override val role: String = "assistant",
    override val content: String? = null,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null
) : Message

@Serializable
data class ChatMessage(
    override val role: String,
    override val content: String?,
    @SerialName("tool_calls")
    val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id")
    val toolCallId: String? = null,
    val name: String? = null
) : Message