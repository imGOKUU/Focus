package com.demo.myapplication.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.GoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.data.local.entity.Session

@Database(
    entities = [Goal::class, BlockedApp::class, DailyGoal::class, Session::class],
    version = 1,
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
