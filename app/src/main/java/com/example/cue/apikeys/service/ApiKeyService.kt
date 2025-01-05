package com.example.cue.apikeys.service

import com.example.cue.apikeys.models.ApiKey
import com.example.cue.network.NetworkClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyService @Inject constructor(
    private val networkClient: NetworkClient
) {
    suspend fun listKeys(): List<ApiKey> {
        return networkClient.get("api-keys")
    }

    suspend fun getKey(id: String): ApiKey {
        return networkClient.get("api-keys/$id")
    }

    suspend fun createKey(name: String, secret: String): ApiKey {
        return networkClient.post("api-keys") {
            put("name", name)
            put("secret", secret)
        }
    }

    suspend fun updateKey(id: String, name: String): ApiKey {
        return networkClient.patch("api-keys/$id") {
            put("name", name)
        }
    }

    suspend fun deleteKey(id: String) {
        networkClient.delete("api-keys/$id")
    }
}