package com.meltide.app.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meltide.app.data.local.entity.GoalStatus
import com.meltide.app.data.local.entity.SessionStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Every migration must carry real user data across intact. Each test builds
 * a database at an old version from its exported schema (app/schemas), fills
 * it with rows the way the app would have, migrates, and checks Room accepts
 * the result and the rows are all still there.
 */
@RunWith(AndroidJUnit4::class)
class FocusMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FocusDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun migrate2To3_keepsGoalsHistoryAndActiveSession() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("INSERT INTO goals (id, name, dailyDurationSeconds, icon, color, isActive) VALUES (1, 'Maths', 3600, 'calculate', 'teal', 1)")
            db.execSQL("INSERT INTO goals (id, name, dailyDurationSeconds, icon, color, isActive) VALUES (2, 'Old goal', 1800, 'code', 'blue', 0)")
            db.execSQL("INSERT INTO blocked_apps (id, goalId, packageName, appName) VALUES (1, 1, 'com.instagram.android', 'Instagram')")
            db.execSQL("INSERT INTO daily_goals (id, goalId, date, allocatedSeconds, completedSeconds, status) VALUES (1, 1, ${today.minusDays(1).toEpochDay()}, 3600, 3600, 'COMPLETED')")
            db.execSQL("INSERT INTO daily_goals (id, goalId, date, allocatedSeconds, completedSeconds, status) VALUES (2, 1, ${today.toEpochDay()}, 3600, 1200, 'PENDING')")
            db.execSQL("INSERT INTO sessions (id, dailyGoalId, startedAt, endedAt, allocatedSeconds, elapsedSeconds, runningSince, completedSeconds, status) VALUES (1, 2, 1000, NULL, 2400, 300, NULL, 0, 'PAUSED')")
        }

        // Fails if the migrated schema doesn't match what Room expects for v3.
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, FocusMigrations.MIGRATION_2_3)

        db.query("SELECT COUNT(*) FROM goals").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.query("SELECT COUNT(*) FROM daily_goals").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.query("SELECT status, elapsedSeconds FROM sessions WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("PAUSED", it.getString(0))
            assertEquals(300, it.getInt(1))
        }
        val indexes = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND name LIKE 'index_%'").use {
            while (it.moveToNext()) indexes += it.getString(0)
        }
        assertTrue(indexes.containsAll(listOf(
            "index_daily_goals_goalId_date", "index_daily_goals_date",
            "index_sessions_dailyGoalId", "index_sessions_status", "index_blocked_apps_goalId"
        )))
        db.close()
    }

    /** The full chain, opened the way the app opens it, read back through the DAOs. */
    @Test
    fun allMigrations_openWithRoomAndReadThroughDaos() = runBlocking {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL("INSERT INTO goals (id, name, dailyDurationSeconds, icon, color, isActive) VALUES (1, 'Maths', 3600, 'calculate', 'teal', 1)")
            db.execSQL("INSERT INTO daily_goals (id, goalId, date, allocatedSeconds, completedSeconds, status) VALUES (1, 1, ${today.toEpochDay()}, 3600, 3600, 'COMPLETED')")
            db.execSQL("INSERT INTO sessions (id, dailyGoalId, startedAt, endedAt, allocatedSeconds, elapsedSeconds, runningSince, completedSeconds, status) VALUES (1, 1, 1000, 4600, 3600, 3600, NULL, 3600, 'COMPLETED')")
        }

        val room = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FocusDatabase::class.java,
            TEST_DB
        ).addMigrations(*FocusMigrations.ALL).build()
        try {
            val goal = room.goalDao().getGoalById(1)
            assertNotNull(goal)
            assertEquals("Maths", goal!!.name)
            val daily = room.dailyGoalDao().getForGoalAndDate(1, today)
            assertEquals(GoalStatus.COMPLETED, daily!!.status)
            assertEquals(SessionStatus.COMPLETED, room.sessionDao().getById(1)!!.status)
        } finally {
            room.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
