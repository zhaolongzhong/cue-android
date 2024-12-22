package com.example.cue.settings.apikeys

enum class ApiKeyType(
    val key: String,
    val displayName: String,
    val placeholder: String,
) {
    OPENAI(
        key = "OPENAI_API_KEY",
        displayName = "OpenAI",
        placeholder = "sk-...",
    ),
    ANTHROPIC(
        key = "ANTHROPIC_API_KEY",
        displayName = "Anthropic",
        placeholder = "sk-ant-...",
    ),
    GEMINI(
        key = "GEMINI_API_KEY",
        displayName = "Google Gemini",
        placeholder = "...",
    ),
    ;

    companion object {
        val values = values().toList()
    }
}
