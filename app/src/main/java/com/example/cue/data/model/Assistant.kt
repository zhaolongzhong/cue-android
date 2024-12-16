package com.example.cue.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class AssistantCreate(
    val name: String
)

@JsonClass(generateAdapter = true)
data class AssistantMetadata(
    @Json(name = "is_primary") val isPrimary: Boolean? = null,
    val model: String? = null,
    val instruction: String? = null,
    val description: String? = null,
    @Json(name = "max_turns") val maxTurns: Int? = null,
    val context: Map<String, Any>? = null,
    val tools: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AssistantMetadataUpdate(
    @Json(name = "is_primary") val isPrimary: Boolean? = null,
    val model: String? = null,
    val instruction: String? = null,
    val description: String? = null,
    @Json(name = "max_turns") val maxTurns: Int? = null,
    val context: Map<String, Any>? = null,
    val tools: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class Assistant(
    val id: String,
    val name: String,
    @Json(name = "created_at") val createdAt: Date,
    @Json(name = "updated_at") val updatedAt: Date,
    val metadata: AssistantMetadata? = null
) {
    val isPrimary: Boolean
        get() = metadata?.isPrimary == true
}