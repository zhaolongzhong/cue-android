package com.example.cue.chat.models


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonAdapter

@JsonClass(generateAdapter = true)
data class ConversationModel(
    @Json(name = "id") val id: String,
    @Json(name = "assistant_id") val assistantId: String,
    @Json(name = "title") val title: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)