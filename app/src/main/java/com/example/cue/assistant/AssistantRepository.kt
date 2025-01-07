package com.example.cue.assistant

import com.example.cue.assistant.models.Assistant
import com.example.cue.assistant.models.AssistantCreationParams
import com.example.cue.network.NetworkClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantRepository @Inject constructor(
    private val networkClient: NetworkClient
) {
    suspend fun getAssistants(): Result<List<Assistant>> = runCatching {
        networkClient.get<List<Assistant>>("assistants")
    }

    suspend fun getAssistant(id: String): Result<Assistant> = runCatching {
        networkClient.get<Assistant>("assistants/$id")
    }

    suspend fun createAssistant(params: AssistantCreationParams): Result<Assistant> = runCatching {
        networkClient.post<Assistant>("assistants") {
            setBody(params)
        }
    }

    suspend fun updateAssistant(
        id: String,
        params: AssistantCreationParams
    ): Result<Assistant> = runCatching {
        networkClient.put<Assistant>("assistants/$id") {
            setBody(params)
        }
    }

    suspend fun deleteAssistant(id: String): Result<Unit> = runCatching {
        networkClient.delete<Unit>("assistants/$id")
    }
}