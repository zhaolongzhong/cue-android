package com.example.cue.assistants.service

import com.example.cue.assistants.models.Assistant
import com.example.cue.assistants.models.AssistantMetadataUpdate
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import javax.inject.Inject
import javax.inject.Singleton

sealed class AssistantError : Exception() {
    object NetworkError : AssistantError()
    object InvalidResponse : AssistantError()
    object NotFound : AssistantError()
    object Unknown : AssistantError()
}

@Singleton
class AssistantService @Inject constructor(
    private val networkClient: NetworkClient
) {
    suspend fun createAssistant(name: String? = null, isPrimary: Boolean = false): Result<Assistant> = runCatching {
        val assistantName = name ?: "Untitled"
        networkClient.request(
            AssistantEndpoint.create(name = assistantName, isPrimary = isPrimary)
        )
    }.mapError()

    suspend fun getAssistant(id: String): Result<Assistant> = runCatching {
        networkClient.request(
            AssistantEndpoint.get(id = id)
        )
    }.mapError()

    suspend fun listAssistants(skip: Int = 0, limit: Int = 5): Result<List<Assistant>> = runCatching {
        networkClient.request(
            AssistantEndpoint.list(skip = skip, limit = limit)
        )
    }.mapError()

    suspend fun deleteAssistant(id: String): Result<Unit> = runCatching {
        networkClient.requestWithEmptyResponse(
            AssistantEndpoint.delete(id = id)
        )
    }.mapError()

    suspend fun updateAssistant(
        id: String, 
        name: String? = null,
        metadata: AssistantMetadataUpdate? = null
    ): Result<Assistant> = runCatching {
        networkClient.request(
            AssistantEndpoint.update(id = id, name = name, metadata = metadata)
        )
    }.mapError()

    private fun <T> Result<T>.mapError(): Result<T> = mapError { error ->
        when (error) {
            is NetworkError -> when (error) {
                is NetworkError.HttpError -> when (error.code) {
                    404 -> AssistantError.NotFound
                    else -> AssistantError.NetworkError
                }
                else -> AssistantError.NetworkError
            }
            else -> AssistantError.Unknown
        }
    }
}

sealed class AssistantEndpoint {
    companion object {
        fun create(name: String, isPrimary: Boolean) = object : AssistantEndpoint() {
            val body = mapOf(
                "name" to name,
                "metadata" to mapOf("is_primary" to isPrimary)
            )
        }

        fun get(id: String) = object : AssistantEndpoint() {
            val path = "assistants/$id"
        }

        fun list(skip: Int, limit: Int) = object : AssistantEndpoint() {
            val path = "assistants?skip=$skip&limit=$limit"
        }

        fun delete(id: String) = object : AssistantEndpoint() {
            val path = "assistants/$id"
            val method = "DELETE"
        }

        fun update(id: String, name: String?, metadata: AssistantMetadataUpdate?) = object : AssistantEndpoint() {
            val path = "assistants/$id"
            val method = "PUT"
            val body = buildMap {
                name?.let { put("name", it) }
                metadata?.let { put("metadata", it) }
            }
        }
    }
}