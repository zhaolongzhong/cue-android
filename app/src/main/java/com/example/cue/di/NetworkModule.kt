package com.example.cue.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.cue.BuildConfig
import com.example.cue.Environment
import com.example.cue.anthropic.AnthropicClient
import com.example.cue.auth.TokenManager
import com.example.cue.debug.Provider
import com.example.cue.network.InstantAdapter
import com.example.cue.network.JsonValueAdapter
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkClientImpl
import com.example.cue.network.NetworkError
import com.example.cue.network.UnitAdapter
import com.example.cue.openai.OpenAIClient
import com.example.cue.settings.apikeys.ApiKeyType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences {
        return context.getSharedPreferences("CuePreferences", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(Unit::class.java, UnitAdapter())
            .add(JsonValueAdapter())
            .add(InstantAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                try {
                    chain.proceed(chain.request())
                } catch (e: Exception) {
                    throw NetworkError.NetworkFailure(
                        message = "Network request failed: ${e.localizedMessage}",
                    )
                }
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .build()
    }

    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(): String {
        val baseUrl = BuildConfig.API_BASE_URL.takeIf { it.isNotEmpty() } ?: Environment.DEFAULT_API_BASE_URL
        return "$baseUrl/api/v1"
    }

    @Provides
    @Singleton
    @Named("websocketUrl")
    fun provideWebsocketUrl(): String {
        val baseUrl = BuildConfig.WEBSOCKET_BASE_URL.takeIf { it.isNotEmpty() } ?: Environment.DEFAULT_WEBSOCKET_BASE_URL
        return "$baseUrl/api/v1/ws"
    }

    @Provides
    @Singleton
    fun provideNetworkClient(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        @Named("baseUrl") baseUrl: String,
        sharedPreferences: SharedPreferences,
    ): NetworkClient {
        return NetworkClientImpl(
            okHttpClient = okHttpClient,
            moshi = moshi,
            baseUrl = baseUrl,
            sharedPreferences = sharedPreferences,
        )
    }

    @Provides
    @Singleton
    @Named("openaiBaseUrl")
    fun provideOpenAIBaseUrl(): String {
        return "https://api.openai.com/v1"
    }

    @Provides
    @Singleton
    @Named("openaiOkHttpClient")
    fun provideOpenAIOkHttpClient(sharedPreferences: SharedPreferences): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val apiKey = sharedPreferences.getString(ApiKeyType.OPENAI.key, "") ?: ""
                if (apiKey.isEmpty()) {
                    throw NetworkError.NetworkFailure(
                        message = "OpenAI API key not found in preferences",
                    )
                }

                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                try {
                    chain.proceed(request)
                } catch (e: Exception) {
                    throw NetworkError.NetworkFailure(
                        message = "Network request failed: ${e.localizedMessage}",
                    )
                }
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenAIClient(sharedPreferences: SharedPreferences): OpenAIClient {
        val apiKey = sharedPreferences.getString(ApiKeyType.OPENAI.key, "") ?: ""
        return OpenAIClient(apiKey)
    }

    @Provides
    @Singleton
    fun provideAnthropicClient(
        tokenManager: TokenManager,
        @Named("baseUrl") baseUrl: String,
        sharedPreferences: SharedPreferences,
    ): AnthropicClient {
        val accessToken = tokenManager.getAccessToken() ?: ""
        return AnthropicClient(accessToken, baseUrl, sharedPreferences)
    }

    @Provides
    @Singleton
    fun provideAnthropicModel(
        tokenManager: TokenManager,
        sharedPreferences: SharedPreferences,
        @Named("baseUrl") baseUrl: String,
    ): com.example.cue.anthropic.AnthropicModel {
        // Check runtime provider selection from SharedPreferences
        val providerString = sharedPreferences.getString("debug_current_provider", null)
        val currentProvider = if (providerString != null) {
            Provider.fromString(providerString) ?: Provider.ANTHROPIC
        } else {
            // Default to ANTHROPIC if no provider selected
            Provider.ANTHROPIC
        }

        val useBackendProxy = (currentProvider == Provider.CUE)

        Log.d("NetworkModule", "Current provider: $currentProvider, useBackendProxy: $useBackendProxy")

        if (useBackendProxy) {
            Log.d("NetworkModule", "Using backend proxy mode (CUE provider)")

            // Backend proxy mode: Use custom HttpClient to proxy requests
            val authToken = tokenManager.getAccessToken() ?: ""

            // Remove the /api/v1 suffix for base URL
            return com.example.cue.anthropic.AnthropicModel.fromDynamicBackendProxy(
                baseUrlProvider = { baseUrl.removeSuffix("/api/v1") },
                authTokenProvider = { authToken },
                sharedPreferences = sharedPreferences,
            )
        } else {
            Log.d("NetworkModule", "Using direct API mode (ANTHROPIC provider)")

            // Direct mode: Connect to official Anthropic API
            return com.example.cue.anthropic.AnthropicModel.fromDynamicApiKey {
                val userApiKey = sharedPreferences.getString(ApiKeyType.ANTHROPIC.key, "")
                val configApiKey = BuildConfig.ANTHROPIC_API_KEY.takeIf { it.isNotEmpty() }

                val apiKey = when {
                    !userApiKey.isNullOrEmpty() -> {
                        Log.d("NetworkModule", "Using Anthropic API key from user settings (${userApiKey.take(10)}...)")
                        userApiKey
                    }
                    !configApiKey.isNullOrEmpty() -> {
                        Log.d("NetworkModule", "Using Anthropic API key from local.properties (${configApiKey.take(10)}...)")
                        configApiKey
                    }
                    else -> {
                        Log.w("NetworkModule", "No valid Anthropic API key found! Please set it in Settings > API Keys or local.properties")
                        return@fromDynamicApiKey null
                    }
                }

                Log.d("NetworkModule", "Using AnthropicModel in direct mode with official API")
                apiKey
            }
        }
    }

    @Provides
    @Singleton
    @Named("useAnthropicSdk")
    fun provideUseAnthropicSdk(): Boolean {
        // Always use AnthropicModel now that it supports dynamic provider switching
        return true
    }
}
