package ai.plusonelabs.di

import ai.plusonelabs.utils.SharedPreferencesManager
import android.content.Context
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
