package com.example.cue.assistants.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Assistant(
    val id: String,
    val name: String,
    val metadata: AssistantMetadata? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class AssistantMetadata(
    @Json(name = "is_primary") val isPrimary: Boolean = false,
    val model: String? = null,
    val instruction: String? = null,
    val description: String? = null,
    @Json(name = "max_turns") val maxTurns: Int? = null,
    val context: String? = null,
    val tools: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AssistantMetadataUpdate(
    @Json(name = "is_primary") val isPrimary: Boolean? = null,
    val model: String? = null,
    val instruction: String? = null,
    val description: String? = null,
    @Json(name = "max_turns") val maxTurns: Int? = null,
    val context: String? = null,
    val tools: List<String>? = null
)