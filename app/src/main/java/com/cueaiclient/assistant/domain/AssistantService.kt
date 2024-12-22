package com.cueaiclient.assistant.domain

import com.cueaiclient.assistant.domain.model.Assistant
import com.cueaiclient.assistant.domain.model.AssistantMetadataUpdate
import com.cueaiclient.assistant.domain.model.ConversationModel
import com.cueaiclient.assistant.domain.model.MessageModel
import com.cueaiclient.core.util.Result

interface AssistantService {
    suspend fun createAssistant(name: String? = null, isPrimary: Boolean = false): Result<Assistant>
    
    suspend fun getAssistant(id: String): Result<Assistant>
    
    suspend fun listAssistants(skip: Int = 0, limit: Int = 5): Result<List<Assistant>>
    
    suspend fun deleteAssistant(id: String): Result<Unit>
    
    suspend fun updateAssistant(
        id: String,
        name: String? = null,
        metadata: AssistantMetadataUpdate? = null
    ): Result<Assistant>
    
    suspend fun listAssistantConversations(
        id: String,
        isPrimary: Boolean? = null,
        skip: Int = 0,
        limit: Int = 10
    ): Result<List<ConversationModel>>
    
    suspend fun listMessages(
        conversationId: String,
        skip: Int = 0,
        limit: Int = 20
    ): Result<List<MessageModel>>
    
    suspend fun getMessage(id: String): Result<MessageModel?>
    
    suspend fun createPrimaryConversation(
        assistantId: String,
        name: String? = "default"
    ): Result<ConversationModel>
}