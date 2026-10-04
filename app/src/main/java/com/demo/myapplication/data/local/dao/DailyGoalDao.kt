package com.demo.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.demo.myapplication.data.local.entity.DailyGoal
import kotlinx.coroutines.flow.Flow
import java.sql.Date
import java.time.LocalDate

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals WHERE date = :date ORDER BY id ASC")
    fun observeForDate(date: LocalDate): Flow<List<DailyGoal>>

    @Query("SELECT * FROM daily_goals WHERE id = :id")
    suspend fun getById(id: Long): DailyGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyGoal: DailyGoal): Long

    @Update
    suspend fun update(dailyGoal: DailyGoal)

    @Query("SELECT * FROM daily_goals WHERE goalId=:goalId AND date=:date LIMIT 1")
    suspend fun getForGoalAndDate(goalId: Long,date: LocalDate): DailyGoal?

    // Used by the "generate today's goals" logic to avoid duplicating
    // a DailyGoal for the same Goal on the same date.
    @Query("SELECT COUNT(*) FROM daily_goals WHERE goalId = :goalId AND date = :date")
    suspend fun countForGoalOnDate(goalId: Long, date: LocalDate): Int
}
