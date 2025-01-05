package com.example.cue.apikeys.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class ApiKey(
    val id: String,
    val name: String,
    val secret: String,
    val scopes: List<String>,
    @Json(name = "created_at")
    val createdAt: Date,
    @Json(name = "expires_at")
    val expiresAt: Date?,
    @Json(name = "last_used_at")
    val lastUsedAt: Date?,
    @Json(name = "is_active")
    val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class ApiKeyPrivate(
    val id: String,
    val name: String,
    val secret: String,
    @Json(name = "user_id")
    val userId: String,
    val scopes: List<String>,
    @Json(name = "created_at")
    val createdAt: Date,
    @Json(name = "expires_at")
    val expiresAt: Date?,
    @Json(name = "last_used_at")
    val lastUsedAt: Date?,
    @Json(name = "is_active")
    val isActive: Boolean
) {
    fun toPublicKey(): ApiKey = ApiKey(
        id = id,
        name = name,
        secret = secret,
        scopes = scopes,
        createdAt = createdAt,
        expiresAt = expiresAt,
        lastUsedAt = lastUsedAt,
        isActive = isActive
    )
}

enum class ApiKeyType(val displayName: String) {
    OPENAI("OpenAI API Key"),
    ANTHROPIC("Anthropic API Key"),
    GEMINI("Google AI API Key");
}