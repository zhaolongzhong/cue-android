package com.example.cue.data.repository

import com.example.cue.data.api.AssistantApi
import com.example.cue.data.model.Assistant
import com.example.cue.data.model.AssistantCreate
import com.example.cue.data.model.AssistantMetadataUpdate
import com.example.cue.util.NetworkResult
import com.example.cue.util.safeApiCall
import javax.inject.Inject

class AssistantRepository @Inject constructor(
    private val assistantApi: AssistantApi
) {
    suspend fun listAssistants(): NetworkResult<List<Assistant>> =
        safeApiCall { assistantApi.listAssistants() }

    suspend fun getAssistant(id: String): NetworkResult<Assistant> =
        safeApiCall { assistantApi.getAssistant(id) }

    suspend fun createAssistant(assistant: AssistantCreate): NetworkResult<Assistant> =
        safeApiCall { assistantApi.createAssistant(assistant) }

    suspend fun updateAssistant(
        id: String,
        metadata: AssistantMetadataUpdate
    ): NetworkResult<Assistant> =
        safeApiCall { assistantApi.updateAssistant(id, metadata) }

    suspend fun deleteAssistant(id: String): NetworkResult<Unit> =
        safeApiCall { assistantApi.deleteAssistant(id) }
}