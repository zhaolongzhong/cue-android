package com.example.cue.di

import android.content.Context
import com.example.cue.utils.SharedPreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSharedPreferencesManager(
        @ApplicationContext context: Context,
    ): SharedPreferencesManager = SharedPreferencesManager(context)
}
