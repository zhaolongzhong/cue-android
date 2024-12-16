package com.example.cue.anthropic

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class ContentType {
    @Json(name = "text") TEXT,
    @Json(name = "tool_use") TOOL_USE,
    @Json(name = "tool_result") TOOL_RESULT,
    @Json(name = "image") IMAGE
}

@JsonClass(generateAdapter = true)
data class TextBlock(
    @Json(name = "text") val text: String,
    @Json(name = "type") val type: String = "text"
)

@JsonClass(generateAdapter = true)
data class ToolUseBlock(
    @Json(name = "type") val type: String = "tool_use",
    @Json(name = "id") val id: String,
    @Json(name = "input") val input: Map<String, Any>,
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class ImageBlock(
    @Json(name = "type") val type: String = "image",
    @Json(name = "media_type") val mediaType: String,
    @Json(name = "data") val data: String
)

sealed class ContentBlock {
    data class Text(val block: TextBlock) : ContentBlock()
    data class ToolUse(val block: ToolUseBlock) : ContentBlock()
    data class Image(val block: ImageBlock) : ContentBlock()
    
    val text: String
        get() = when (this) {
            is Text -> block.text
            is ToolUse -> block.toString()
            is Image -> "image"
        }
}

@JsonClass(generateAdapter = true)
data class ToolResultContent(
    @Json(name = "is_error") val isError: Boolean,
    @Json(name = "tool_use_id") val toolUseId: String,
    @Json(name = "type") val type: String = "tool_result",
    @Json(name = "content") val content: List<ContentBlock>
)

@JsonClass(generateAdapter = true)
data class MessageParam(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: List<ContentBlock>
)

@JsonClass(generateAdapter = true)
data class ToolResultMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: List<ToolResultContent>
)

enum class StopReason {
    @Json(name = "end_turn") END_TURN,
    @Json(name = "max_tokens") MAX_TOKENS,
    @Json(name = "stop_sequence") STOP_SEQUENCE,
    @Json(name = "tool_use") TOOL_USE
}

@JsonClass(generateAdapter = true)
data class PromptCachingBetaUsage(
    @Json(name = "cache_creation_input_tokens") val cacheCreationInputTokens: Int?,
    @Json(name = "cache_read_input_tokens") val cacheReadInputTokens: Int?,
    @Json(name = "input_tokens") val inputTokens: Int?,
    @Json(name = "output_tokens") val outputTokens: Int?
)

@JsonClass(generateAdapter = true)
data class AnthropicMessage(
    @Json(name = "id") val id: String,
    @Json(name = "content") val content: List<ContentBlock>,
    @Json(name = "model") val model: String,
    @Json(name = "role") val role: String,
    @Json(name = "stop_reason") val stopReason: String?,
    @Json(name = "stop_sequence") val stopSequence: String?,
    @Json(name = "type") val type: String,
    @Json(name = "usage") val usage: PromptCachingBetaUsage
)