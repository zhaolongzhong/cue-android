package com.example.cue.chat.repository

import com.example.cue.chat.models.MessageModel
import com.example.cue.network.AssistantService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val assistantService: AssistantService,
    private val messageModelStore: MessageModelStore
) {
    private val messageFlows = mutableMapOf<String, MutableSharedFlow<MessageModel>>()

    suspend fun saveMessage(messageModel: MessageModel): MessageModel {
        val savedMessage = messageModelStore.save(messageModel)
        emitMessage(savedMessage)
        return savedMessage
    }

    suspend fun listMessages(
        conversationId: String,
        skip: Int,
        limit: Int
    ): List<MessageModel> {
        return assistantService.listMessages(conversationId, skip, limit)
    }

    suspend fun getMessage(id: String): MessageModel? {
        return assistantService.getMessage(id)
    }

    suspend fun fetchAllMessages(conversationId: String): List<MessageModel> {
        return messageModelStore.fetchAllMessages(conversationId)
    }

    fun messageStream(conversationId: String): Flow<MessageModel> {
        return messageFlows.getOrPut(conversationId) {
            MutableSharedFlow(replay = 0, extraBufferCapacity = 64)
        }
    }

    private suspend fun emitMessage(message: MessageModel) {
        messageFlows[message.conversationId]?.emit(message)
    }

    fun removeMessageStream(conversationId: String) {
        messageFlows.remove(conversationId)
    }
}

@Singleton
class MessageModelStore @Inject constructor() {
    private val messages = mutableMapOf<String, MutableList<MessageModel>>()

    suspend fun save(messageModel: MessageModel): MessageModel {
        messages.getOrPut(messageModel.conversationId) { mutableListOf() }
            .add(messageModel)
        return messageModel
    }

    suspend fun fetchAllMessages(conversationId: String): List<MessageModel> {
        return messages[conversationId] ?: emptyList()
    }
}