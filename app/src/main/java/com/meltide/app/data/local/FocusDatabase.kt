package com.meltide.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meltide.app.data.local.dao.DailyGoalDao
import com.meltide.app.data.local.dao.GoalDao
import com.meltide.app.data.local.dao.SessionDao
import com.meltide.app.data.local.entity.BlockedApp
import com.meltide.app.data.local.entity.DailyGoal
import com.meltide.app.data.local.entity.Goal
import com.meltide.app.data.local.entity.Session

@Database(
    entities = [Goal::class, BlockedApp::class, DailyGoal::class, Session::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "focus_app.db"
    }
}
