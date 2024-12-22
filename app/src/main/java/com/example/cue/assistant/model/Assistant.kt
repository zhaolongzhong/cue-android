package com.example.cue.assistant.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Assistant(
    val id: String,
    val name: String?,
    val metadata: AssistantMetadata? = null
)

@JsonClass(generateAdapter = true)
data class AssistantMetadata(
    val description: String? = null,
    val capabilities: List<String>? = null,
    val model: String? = null
)

sealed class AssistantError(message: String) : Exception(message) {
    class LoadError(message: String) : AssistantError(message)
    class CreateError(message: String) : AssistantError(message)
    class UpdateError(message: String) : AssistantError(message)
    class DeleteError(message: String) : AssistantError(message)
}