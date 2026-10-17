package com.meltide.app.di

import android.content.Context
import androidx.room.Room
import com.meltide.app.data.local.FocusDatabase
import com.meltide.app.data.local.FocusMigrations
import com.meltide.app.data.local.dao.DailyGoalDao
import com.meltide.app.data.local.dao.GoalDao
import com.meltide.app.data.local.dao.SessionDao
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
            // Every schema change ships with a Migration (see Migrations.kt);
            // there is deliberately no destructive fallback for upgrades - a
            // missing migration fails loudly in testing instead of silently
            // wiping users' goals and history.
            .addMigrations(*FocusMigrations.ALL)
            // Only reachable by installing an older build over a newer one
            // (development); Play never downgrades an app.
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    fun provideGoalDao(db: FocusDatabase): GoalDao = db.goalDao()

    @Provides
    fun provideDailyGoalDao(db: FocusDatabase): DailyGoalDao = db.dailyGoalDao()

    @Provides
    fun provideSessionDao(db: FocusDatabase): SessionDao = db.sessionDao()
}
