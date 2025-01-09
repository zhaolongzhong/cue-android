package com.example.cue.settings.apikeys

import com.example.cue.settings.apikeys.models.ApiKey
import com.example.cue.settings.apikeys.models.ApiKeyPrivate
import java.util.Date
import javax.inject.Inject
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkError
import com.example.cue.auth.AuthError
import timber.log.Timber

interface ApiKeyService {
    suspend fun createApiKey(
        name: String,
        keyType: String,
        scopes: List<String>?,
        expiresAt: Date?
    ): Result<ApiKeyPrivate>

    suspend fun listApiKeys(skip: Int, limit: Int): Result<List<ApiKey>>

    suspend fun getApiKey(id: String): Result<ApiKey>

    suspend fun updateApiKey(
        id: String,
        name: String?,
        scopes: List<String>?,
        expiresAt: Date?,
        isActive: Boolean?
    ): Result<ApiKey>

    suspend fun deleteApiKey(id: String): Result<ApiKey>
}

class ApiKeyServiceImpl @Inject constructor(
    private val networkClient: NetworkClient
) : ApiKeyService {

    override suspend fun createApiKey(
        name: String,
        keyType: String,
        scopes: List<String>?,
        expiresAt: Date?
    ): Result<ApiKeyPrivate> = runCatching {
        try {
            networkClient.request(
                ApiKeysEndpoint.Create(
                    name = name,
                    keyType = keyType,
                    scopes = scopes,
                    expiresAt = expiresAt
                )
            )
        } catch (e: NetworkError.Unauthorized) {
            throw AuthError.Unauthorized
        } catch (e: Exception) {
            Timber.e(e, "Create API key error")
            throw AuthError.NetworkError
        }
    }

    override suspend fun listApiKeys(skip: Int, limit: Int): Result<List<ApiKey>> = runCatching {
        try {
            networkClient.request(ApiKeysEndpoint.List(skip = skip, limit = limit))
        } catch (e: NetworkError.Unauthorized) {
            throw AuthError.Unauthorized
        } catch (e: Exception) {
            Timber.e(e, "List API keys error")
            throw AuthError.NetworkError
        }
    }

    override suspend fun getApiKey(id: String): Result<ApiKey> = runCatching {
        try {
            networkClient.request(ApiKeysEndpoint.Get(id = id))
        } catch (e: NetworkError.Unauthorized) {
            throw AuthError.Unauthorized
        } catch (e: NetworkError.HttpError) {
            if (e.code == 404) throw AuthError.InvalidResponse
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Get API key error")
            throw AuthError.NetworkError
        }
    }

    override suspend fun updateApiKey(
        id: String,
        name: String?,
        scopes: List<String>?,
        expiresAt: Date?,
        isActive: Boolean?
    ): Result<ApiKey> = runCatching {
        try {
            networkClient.request(
                ApiKeysEndpoint.Update(
                    id = id,
                    name = name,
                    scopes = scopes,
                    expiresAt = expiresAt,
                    isActive = isActive
                )
            )
        } catch (e: NetworkError.Unauthorized) {
            throw AuthError.Unauthorized
        } catch (e: NetworkError.HttpError) {
            if (e.code == 404) throw AuthError.InvalidResponse
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Update API key error")
            throw AuthError.NetworkError
        }
    }

    override suspend fun deleteApiKey(id: String): Result<ApiKey> = runCatching {
        try {
            networkClient.request(ApiKeysEndpoint.Delete(id = id))
        } catch (e: NetworkError.Unauthorized) {
            throw AuthError.Unauthorized
        } catch (e: NetworkError.HttpError) {
            if (e.code == 404) throw AuthError.InvalidResponse
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Delete API key error")
            throw AuthError.NetworkError
        }
    }
}