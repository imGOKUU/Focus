package com.meltide.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema history. Version 2 is the baseline every install has (the first
 * exported schema, app/schemas/.../2.json). Each later version adds one
 * Migration here and a case in FocusMigrationTest - never a destructive
 * fallback, since that would erase users' goals and history.
 */
object FocusMigrations {

    /** 2 -> 3: indexes for the lookups Today, History and sessions run constantly. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_goals_goalId_date` ON `daily_goals` (`goalId`, `date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_goals_date` ON `daily_goals` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_dailyGoalId` ON `sessions` (`dailyGoalId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_sessions_status` ON `sessions` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_blocked_apps_goalId` ON `blocked_apps` (`goalId`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_2_3)
}
