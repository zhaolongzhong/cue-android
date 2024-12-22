package com.example.cue.assistant

import com.example.cue.assistant.model.Assistant
import javax.inject.Inject

interface AssistantService {
    suspend fun getAssistants(): List<Assistant>
    suspend fun getPrimaryAssistant(): Assistant?
    suspend fun createAssistant(name: String): Assistant
    suspend fun deleteAssistant(id: String)
    suspend fun updateAssistantName(id: String, newName: String)
    suspend fun setPrimaryAssistant(id: String)
}

class AssistantServiceImpl @Inject constructor(
    private val networkClient: NetworkClient
) : AssistantService {

    override suspend fun getAssistants(): List<Assistant> {
        return try {
            networkClient.get<List<Assistant>>("assistants")
        } catch (e: Exception) {
            throw AssistantError.LoadError(e.message ?: "Failed to load assistants")
        }
    }

    override suspend fun getPrimaryAssistant(): Assistant? {
        return try {
            networkClient.get<Assistant>("assistants/primary")
        } catch (e: Exception) {
            throw AssistantError.LoadError(e.message ?: "Failed to load primary assistant")
        }
    }

    override suspend fun createAssistant(name: String): Assistant {
        return try {
            networkClient.post<Assistant>("assistants") {
                put("name", name)
            }
        } catch (e: Exception) {
            throw AssistantError.CreateError(e.message ?: "Failed to create assistant")
        }
    }

    override suspend fun deleteAssistant(id: String) {
        try {
            networkClient.delete<Unit>("assistants/$id")
        } catch (e: Exception) {
            throw AssistantError.DeleteError(e.message ?: "Failed to delete assistant")
        }
    }

    override suspend fun updateAssistantName(id: String, newName: String) {
        try {
            networkClient.patch<Unit>("assistants/$id") {
                put("name", newName)
            }
        } catch (e: Exception) {
            throw AssistantError.UpdateError(e.message ?: "Failed to update assistant name")
        }
    }

    override suspend fun setPrimaryAssistant(id: String) {
        try {
            networkClient.put<Unit>("assistants/$id/primary")
        } catch (e: Exception) {
            throw AssistantError.UpdateError(e.message ?: "Failed to set primary assistant")
        }
    }
}