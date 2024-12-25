package com.example.cue.chat.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class MessageModel(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "content") val content: String,
    @Json(name = "sender") val sender: String? = null,
    @Json(name = "recipient") val recipient: String? = null,
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "metadata") val metadata: Map<String, Any>? = null
) {
    constructor(payload: MessagePayload, conversationId: String) : this(
        content = payload.message,
        sender = payload.sender,
        recipient = payload.recipient,
        conversationId = conversationId,
        metadata = payload.metadata
    )
}

@JsonClass(generateAdapter = true)
data class MessagePayload(
    @Json(name = "message") val message: String,
    @Json(name = "sender") val sender: String? = null,
    @Json(name = "recipient") val recipient: String? = null,
    @Json(name = "websocket_request_id") val websocketRequestId: String? = null,
    @Json(name = "metadata") val metadata: Map<String, Any>? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "payload") val payload: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ConversationModel(
    @Json(name = "id") val id: String,
    @Json(name = "assistant_id") val assistantId: String,
    @Json(name = "name") val name: String?,
    @Json(name = "is_primary") val isPrimary: Boolean,
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)