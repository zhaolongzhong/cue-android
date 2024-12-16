package com.example.cue.data.api

import com.example.cue.data.model.Assistant
import com.example.cue.data.model.AssistantCreate
import com.example.cue.data.model.AssistantMetadataUpdate
import retrofit2.Response
import retrofit2.http.*

interface AssistantApi {
    @GET("assistants")
    suspend fun listAssistants(): Response<List<Assistant>>

    @GET("assistants/{id}")
    suspend fun getAssistant(@Path("id") id: String): Response<Assistant>

    @POST("assistants")
    suspend fun createAssistant(@Body assistant: AssistantCreate): Response<Assistant>

    @PATCH("assistants/{id}")
    suspend fun updateAssistant(
        @Path("id") id: String,
        @Body metadata: AssistantMetadataUpdate
    ): Response<Assistant>

    @DELETE("assistants/{id}")
    suspend fun deleteAssistant(@Path("id") id: String): Response<Unit>
}