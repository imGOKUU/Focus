package com.demo.myapplication.di

import com.demo.myapplication.core.platform.InstalledAppsProvider
import com.demo.myapplication.core.platform.SystemInstalledAppsProvider
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