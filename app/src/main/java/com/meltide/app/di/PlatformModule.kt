package com.meltide.app.di

import com.meltide.app.core.platform.InstalledAppsProvider
import com.meltide.app.core.platform.SystemInstalledAppsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindInstalledAppsProvider(impl: SystemInstalledAppsProvider): InstalledAppsProvider
}