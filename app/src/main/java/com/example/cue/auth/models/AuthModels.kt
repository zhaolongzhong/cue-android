package com.example.cue.auth.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "refresh_token")
    val refreshToken: String?,
)

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id")
    val id: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "name")
    val name: String?,
    @Json(name = "phone_number")
    val phoneNumber: String?,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "updated_at")
    val updatedAt: String?,
    @Json(name = "is_active")
    val isActive: Boolean,
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email")
    val email: String,
    @Json(name = "password")
    val password: String,
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    @Json(name = "email")
    val email: String,
    @Json(name = "password")
    val password: String,
    @Json(name = "invite_code")
    val inviteCode: String?,
)
