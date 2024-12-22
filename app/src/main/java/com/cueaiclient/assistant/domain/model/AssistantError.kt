package com.cueaiclient.assistant.domain.model

sealed class AssistantError(override val message: String) : Exception(message) {
    data object NetworkError : AssistantError("Network error occurred.")
    data object InvalidResponse : AssistantError("Invalid response from server.")
    data object NotFound : AssistantError("Assistant not found.")
    data object Unknown : AssistantError("An unknown error occurred.")
}