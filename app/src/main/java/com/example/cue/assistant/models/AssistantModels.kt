package com.example.cue.assistant.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Assistant(
    @Json(name = "id") val id: String,
    @Json(name = "object") val objectType: String = "assistant",
    @Json(name = "created_at") val createdAt: Long,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "model") val model: String,
    @Json(name = "instructions") val instructions: String?,
    @Json(name = "tools") val tools: List<AssistantTool>,
    @Json(name = "file_ids") val fileIds: List<String>,
    @Json(name = "metadata") val metadata: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class AssistantTool(
    @Json(name = "type") val type: String
)

sealed interface AssistantUiState {
    data object Loading : AssistantUiState
    data class Error(val message: String) : AssistantUiState
    data class Success(val assistants: List<Assistant>) : AssistantUiState
}

data class AssistantCreationParams(
    val name: String,
    val description: String?,
    val instructions: String?,
    val model: String,
    val tools: List<AssistantTool> = emptyList(),
    val fileIds: List<String> = emptyList(),
    val metadata: Map<String, String>? = null
)