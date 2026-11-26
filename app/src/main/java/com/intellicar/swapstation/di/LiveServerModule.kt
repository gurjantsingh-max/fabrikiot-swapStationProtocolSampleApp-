package com.intellicar.swapstation.di

import com.intellicar.swapstation.core.config.LiveServerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LiveServerModule {
    @Provides @Singleton
    fun provideLiveServerConfig(): LiveServerConfig = LiveServerConfig.fromBuildConfig()
}
