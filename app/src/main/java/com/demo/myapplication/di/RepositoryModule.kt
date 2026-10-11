package com.demo.myapplication.di

import com.demo.myapplication.data.repository.GoalRepositoryImpl
import com.demo.myapplication.data.repository.SessionRepositoryImpl
import com.demo.myapplication.domain.repository.GoalRepository
import com.demo.myapplication.domain.repository.SessionRepository
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