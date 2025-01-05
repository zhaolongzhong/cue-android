package com.example.cue.assistant.models

import com.example.cue.network.JsonValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

@JsonClass(generateAdapter = true)
data class Assistant(
    @Json(name = "id") val id: String,
    @Json(name = "object") val objectType: String = "assistant",
    @Json(name = "created_at") val createdAt: Instant,
    @Json(name = "updated_at") val updatedAt: Instant,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "model") val model: String?,
    @Json(name = "instructions") val instructions: String?,
    @Json(name = "tools") val tools: List<AssistantTool>?,
    @Json(name = "metadata") val metadata: JsonValue?,
)

@JsonClass(generateAdapter = true)
data class AssistantTool(
    @Json(name = "type") val type: String,
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
    val metadata: Map<String, String>? = null,
)

@JsonClass(generateAdapter = true)
data class ClientStatus(
    val clientId: String,
    val runnerId: String?,
    val assistantId: String?,
    val isOnline: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis(),
)
