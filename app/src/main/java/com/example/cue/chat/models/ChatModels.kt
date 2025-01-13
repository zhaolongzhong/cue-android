package com.example.cue.chat.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationModel(
    val id: String,
    val title: String,
    @Json(name = "assistant_id") val assistantId: String,
    val metadata: ConversationMetadata?,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
)

@JsonClass(generateAdapter = true)
data class ConversationMetadata(
    @Json(name = "is_primary") val isPrimary: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class MessageModel(
    val id: String,
    @Json(name = "conversation_id") val conversationId: String,
    val content: String,
    val role: String,
    val metadata: MessageMetadata?,
    @Json(name = "created_at") val createdAt: String,
)

@JsonClass(generateAdapter = true)
data class MessageMetadata(
    val name: String? = null,
    val avatar_url: String? = null,
)
