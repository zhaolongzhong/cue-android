package com.example.cue.settings.apikeys

import com.example.cue.network.Endpoint
import com.example.cue.settings.apikeys.models.ApiKey
import com.example.cue.settings.apikeys.models.ApiKeyPrivate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

sealed class ApiKeysEndpoint {
    data class Create(
        val name: String,
        @Json(name = "key_type") val keyType: String,
        val scopes: List<String>?,
        @Json(name = "expires_at") val expiresAt: Date?
    ) : Endpoint<ApiKeyPrivate> {
        override val method = "POST"
        override val path = "api/v1/api-keys"
    }

    data class List(
        val skip: Int,
        val limit: Int
    ) : Endpoint<List<ApiKey>> {
        override val method = "GET"
        override val path = "api/v1/api-keys"
        override val queryParams = mapOf(
            "skip" to skip.toString(),
            "limit" to limit.toString()
        )
    }

    data class Get(val id: String) : Endpoint<ApiKey> {
        override val method = "GET"
        override val path = "api/v1/api-keys/$id"
    }

    @JsonClass(generateAdapter = true)
    data class UpdateRequest(
        val name: String?,
        val scopes: List<String>?,
        @Json(name = "expires_at") val expiresAt: Date?,
        @Json(name = "is_active") val isActive: Boolean?
    )

    data class Update(
        val id: String,
        val name: String?,
        val scopes: List<String>?,
        @Json(name = "expires_at") val expiresAt: Date?,
        @Json(name = "is_active") val isActive: Boolean?
    ) : Endpoint<ApiKey> {
        override val method = "PATCH"
        override val path = "api/v1/api-keys/$id"
        override val body = UpdateRequest(name, scopes, expiresAt, isActive)
    }

    data class Delete(val id: String) : Endpoint<ApiKey> {
        override val method = "DELETE"
        override val path = "api/v1/api-keys/$id"
    }
}