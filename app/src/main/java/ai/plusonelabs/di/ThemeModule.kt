package ai.plusonelabs.di

import ai.plusonelabs.ui.theme.ThemeController
import android.content.Context
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
