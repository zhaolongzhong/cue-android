package com.example.cue.settings.apikeys.di

import com.example.cue.settings.apikeys.ApiKeyService
import com.example.cue.settings.apikeys.ApiKeyServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ApiKeyModule {
    @Binds
    abstract fun bindApiKeyService(
        apiKeyServiceImpl: ApiKeyServiceImpl
    ): ApiKeyService
}