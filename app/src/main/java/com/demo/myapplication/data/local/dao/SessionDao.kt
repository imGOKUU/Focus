package com.demo.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.demo.myapplication.data.local.entity.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE dailyGoalId = :dailyGoalId ORDER BY startedAt DESC")
    fun observeForDailyGoal(dailyGoalId: Long): Flow<List<Session>>

    // Used on app launch / after process death to detect and resume
    // (or reconcile) a session that was still running - spec section 7.
    @Query("SELECT * FROM sessions WHERE status = 'STARTED' LIMIT 1")
    suspend fun getActiveSession(): Session?

    @Query("SELECT * FROM sessions WHERE status = 'STARTED' LIMIT 1")
    fun observeActiveSession(): Flow<Session?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: Session): Long

    @Update
    suspend fun update(session: Session)
}
