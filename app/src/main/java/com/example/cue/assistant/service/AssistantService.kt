package com.example.cue.assistant.service

import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.models.AssistantMetadataUpdate
import com.example.cue.network.NetworkClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantService @Inject constructor(
    private val networkClient: NetworkClient
) {
    suspend fun getAssistant(id: String): Assistant {
        return networkClient.get("assistants/$id")
    }

    suspend fun updateAssistant(id: String, name: String): Assistant {
        return networkClient.patch("assistants/$id") {
            put("name", name)
        }
    }

    suspend fun updateAssistantMetadata(
        id: String,
        metadata: AssistantMetadataUpdate
    ): Assistant {
        return networkClient.patch("assistants/$id/metadata", metadata)
    }

    suspend fun listAssistants(): List<Assistant> {
        return networkClient.get("assistants")
    }

    suspend fun deleteAssistant(id: String) {
        networkClient.delete("assistants/$id")
    }
}