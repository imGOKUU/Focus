package com.meltide.app.di

import com.meltide.app.data.repository.GoalRepositoryImpl
import com.meltide.app.data.repository.SessionRepositoryImpl
import com.meltide.app.domain.repository.GoalRepository
import com.meltide.app.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}