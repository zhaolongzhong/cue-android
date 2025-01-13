package com.example.cue.chat.models


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonAdapter

@JsonClass(generateAdapter = true)
data class MessageModel(
    @Json(name = "id") val id: String,
    @Json(name = "conversation_id") val conversationId: String,
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)