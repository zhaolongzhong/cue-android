package com.example.cue.di

import android.content.Context
import com.example.cue.ui.theme.ThemeController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThemeModule {

    @Provides
    @Singleton
    fun provideThemeController(@ApplicationContext context: Context): ThemeController = ThemeController(context)
}
