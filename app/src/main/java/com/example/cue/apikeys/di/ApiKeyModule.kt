package com.example.cue.apikeys.di

import com.example.cue.apikeys.service.ApiKeyService
import com.example.cue.network.NetworkClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiKeyModule {
    @Provides
    @Singleton
    fun provideApiKeyService(networkClient: NetworkClient): ApiKeyService {
        return ApiKeyService(networkClient)
    }
}