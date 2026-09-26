package com.demo.myapplication.di

import android.content.Context
import androidx.room.Room
import com.demo.myapplication.data.local.FocusDatabase
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.GoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideFocusDatabase(@ApplicationContext context: Context): FocusDatabase =
        Room.databaseBuilder(context, FocusDatabase::class.java, FocusDatabase.DATABASE_NAME)
            // Dev-time only. Replace with real Migration objects before
            // the first release - destructive migration wipes goal history.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideGoalDao(db: FocusDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideDailyGoalDao(db: FocusDatabase): DailyGoalDao = db.dailyGoalDao()

    @Provides
    fun provideSessionDao(db: FocusDatabase): SessionDao = db.sessionDao()
}
