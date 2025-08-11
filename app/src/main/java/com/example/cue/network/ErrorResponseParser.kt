package com.example.cue.network

import org.json.JSONObject

class ErrorResponseParser {

    enum class ApiProvider {
        OPENAI,
        ANTHROPIC,
    }

    fun parseErrorResponse(
        responseCode: Int,
        errorResponse: String?,
        provider: ApiProvider,
    ): String {
        if (errorResponse.isNullOrBlank()) {
            return "API error (code: $responseCode)"
        }

        return try {
            val errorJson = JSONObject(errorResponse)

            when (provider) {
                ApiProvider.OPENAI -> parseOpenAIError(errorJson, responseCode)
                ApiProvider.ANTHROPIC -> parseAnthropicError(errorJson, responseCode)
            }
        } catch (e: Exception) {
            "API error (code: $responseCode): Unable to parse error response"
        }
    }

    private fun parseOpenAIError(errorJson: JSONObject, responseCode: Int): String {
        if (errorJson.has("error")) {
            val error = errorJson.getJSONObject("error")
            val message = error.optString("message", "")
            val type = error.optString("type", "")

            return when {
                message.contains("API key", ignoreCase = true) ->
                    "Authentication failed: Please check your OpenAI API key in settings"
                message.contains("quota", ignoreCase = true) ->
                    "API quota exceeded: Please check your OpenAI account"
                message.contains("rate limit", ignoreCase = true) ->
                    "Rate limit exceeded: Please wait a moment and try again"
                message.isNotEmpty() -> message
                type.isNotEmpty() -> "Error type: $type"
                else -> "API error (code: $responseCode)"
            }
        }

        return "API error (code: $responseCode)"
    }

    private fun parseAnthropicError(errorJson: JSONObject, responseCode: Int): String = when {
        errorJson.has("error") -> {
            val error = when (val errorField = errorJson.get("error")) {
                is JSONObject -> errorField
                is String -> JSONObject().put("message", errorField)
                else -> JSONObject()
            }

            val message = error.optString("message", "")
            val type = error.optString("type", "")

            when {
                message.contains("authentication", ignoreCase = true) ||
                    message.contains("unauthorized", ignoreCase = true) ->
                    "Authentication failed: Please check your access token in settings"
                message.contains("rate", ignoreCase = true) ->
                    "Rate limit exceeded: Please wait a moment and try again"
                message.contains("overloaded", ignoreCase = true) ->
                    "Service temporarily overloaded: Please try again later"
                message.isNotEmpty() -> message
                type.isNotEmpty() -> "Error type: $type"
                else -> "API error (code: $responseCode)"
            }
        }
        errorJson.has("message") -> errorJson.getString("message")
        else -> "API error (code: $responseCode)"
    }
}
