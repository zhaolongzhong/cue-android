package com.example.cue.di

import android.content.Context
import android.content.SharedPreferences
import com.example.cue.BuildConfig
import com.example.cue.network.NetworkClient
import com.example.cue.network.NetworkClientImpl
import com.example.cue.network.NetworkError
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
        return BuildConfig.API_BASE_URL + "/api/v1"
    }

    @Provides
    @Singleton
    @Named("websocketUrl")
    fun provideWebsocketUrl(): String {
        return BuildConfig.WEBSOCKET_BASE_URL + "/api/v1/ws"
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
}
