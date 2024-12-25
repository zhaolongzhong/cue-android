package com.example.cue.chat.models

sealed class ChatError(val message: String) {
    data class ApiError(val errorMessage: String) : ChatError(errorMessage)
    data class ToolError(val errorMessage: String) : ChatError(errorMessage)
    data class UnknownError(val errorMessage: String) : ChatError(errorMessage)
}

object ErrorLogger {
    fun log(error: ChatError) {
        val logMessage = when (error) {
            is ChatError.ApiError -> "API Error: ${error.message}"
            is ChatError.ToolError -> "Tool Error: ${error.message}"
            is ChatError.UnknownError -> "Unknown Error: ${error.message}"
        }
        // Use your logging system here
        println(logMessage)
    }
}