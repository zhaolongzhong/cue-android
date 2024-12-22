package com.cueaiclient.assistant.data

import android.util.Log
import com.cueaiclient.assistant.data.network.AssistantApi
import com.cueaiclient.assistant.domain.AssistantService
import com.cueaiclient.assistant.domain.model.*
import com.cueaiclient.core.util.Result
import com.cueaiclient.core.util.NetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AssistantService"

@Singleton
class AssistantServiceImpl @Inject constructor(
    private val assistantApi: AssistantApi
) : AssistantService {

    override suspend fun createAssistant(name: String?, isPrimary: Boolean): Result<Assistant> =
        withContext(Dispatchers.IO) {
            try {
                val assistantName = name ?: "Untitled"
                val response = assistantApi.createAssistant(assistantName, isPrimary)
                Result.Success(response)
            } catch (e: NetworkError) {
                Log.e(TAG, "Error creating assistant", e)
                Result.Error(mapNetworkError(e))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error creating assistant", e)
                Result.Error(AssistantError.Unknown)
            }
        }

    override suspend fun getAssistant(id: String): Result<Assistant> =
        withContext(Dispatchers.IO) {
            try {
                val response = assistantApi.getAssistant(id)
                Result.Success(response)
            } catch (e: NetworkError) {
                Log.e(TAG, "Error getting assistant", e)
                when (e) {
                    is NetworkError.HttpError -> {
                        if (e.code == 404) Result.Error(AssistantError.NotFound)
                        else Result.Error(AssistantError.NetworkError)
                    }
                    else -> Result.Error(AssistantError.NetworkError)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error getting assistant", e)
                Result.Error(AssistantError.Unknown)
            }
        }

    override suspend fun listAssistants(skip: Int, limit: Int): Result<List<Assistant>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Listing assistants with skip: $skip, limit: $limit")
                val response = assistantApi.listAssistants(skip, limit)
                Result.Success(response)
            } catch (e: NetworkError) {
                Log.e(TAG, "Error listing assistants", e)
                Result.Error(mapNetworkError(e))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error listing assistants", e)
                Result.Error(AssistantError.Unknown)
            }
        }

    override suspend fun deleteAssistant(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                assistantApi.deleteAssistant(id)
                Result.Success(Unit)
            } catch (e: NetworkError) {
                Log.e(TAG, "Error deleting assistant", e)
                when (e) {
                    is NetworkError.HttpError -> {
                        if (e.code == 404) Result.Error(AssistantError.NotFound)
                        else Result.Error(AssistantError.NetworkError)
                    }
                    else -> Result.Error(AssistantError.NetworkError)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error deleting assistant", e)
                Result.Error(AssistantError.Unknown)
            }
        }

    override suspend fun updateAssistant(
        id: String,
        name: String?,
        metadata: AssistantMetadataUpdate?
    ): Result<Assistant> = withContext(Dispatchers.IO) {
        try {
            val response = assistantApi.updateAssistant(id, name, metadata)
            Result.Success(response)
        } catch (e: NetworkError) {
            Log.e(TAG, "Error updating assistant", e)
            when (e) {
                is NetworkError.HttpError -> {
                    if (e.code == 404) Result.Error(AssistantError.NotFound)
                    else Result.Error(AssistantError.NetworkError)
                }
                else -> Result.Error(AssistantError.NetworkError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error updating assistant", e)
            Result.Error(AssistantError.Unknown)
        }
    }

    override suspend fun listAssistantConversations(
        id: String,
        isPrimary: Boolean?,
        skip: Int,
        limit: Int
    ): Result<List<ConversationModel>> = withContext(Dispatchers.IO) {
        try {
            val response = assistantApi.listAssistantConversations(id, isPrimary, skip, limit)
            Result.Success(response)
        } catch (e: NetworkError) {
            Log.e(TAG, "Error listing assistant conversations", e)
            Result.Error(mapNetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error listing assistant conversations", e)
            Result.Error(AssistantError.Unknown)
        }
    }

    override suspend fun listMessages(
        conversationId: String,
        skip: Int,
        limit: Int
    ): Result<List<MessageModel>> = withContext(Dispatchers.IO) {
        try {
            val response = assistantApi.listMessages(conversationId, skip, limit)
            Log.d(TAG, "Fetched ${response.size} messages for conversation ID: $conversationId")
            Result.Success(response)
        } catch (e: NetworkError) {
            Log.e(TAG, "Error listing messages", e)
            Result.Error(mapNetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error listing messages", e)
            Result.Error(AssistantError.Unknown)
        }
    }

    override suspend fun getMessage(id: String): Result<MessageModel?> =
        withContext(Dispatchers.IO) {
            try {
                val response = assistantApi.getMessage(id)
                Result.Success(response)
            } catch (e: NetworkError) {
                Log.e(TAG, "Error getting message", e)
                Result.Error(mapNetworkError(e))
            } catch (e: Exception) {
                Log.e(TAG, "Unknown error getting message", e)
                Result.Error(AssistantError.Unknown)
            }
        }

    override suspend fun createPrimaryConversation(
        assistantId: String,
        name: String?
    ): Result<ConversationModel> = withContext(Dispatchers.IO) {
        try {
            val response = assistantApi.createConversation(assistantId, true)
            Result.Success(response)
        } catch (e: NetworkError) {
            Log.e(TAG, "Error creating primary conversation", e)
            Result.Error(mapNetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Unknown error creating primary conversation", e)
            Result.Error(AssistantError.Unknown)
        }
    }

    private fun mapNetworkError(error: NetworkError): AssistantError = when (error) {
        is NetworkError.HttpError -> {
            if (error.code == 404) AssistantError.NotFound
            else AssistantError.NetworkError
        }
        else -> AssistantError.NetworkError
    }
}