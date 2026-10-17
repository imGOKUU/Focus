package com.meltide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meltide.app.data.local.entity.BlockedApp
import com.meltide.app.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY id ASC")
    fun observeActiveGoals(): Flow<List<Goal>>

    // Including deleted (inactive) goals - history still needs their names.
    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun observeAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Long): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: Goal): Long

    @Update
    suspend fun update(goal: Goal)

    // Deactivating (not deleting) preserves historical session data - spec section 4.
    @Query("UPDATE goals SET isActive = 0 WHERE id = :goalId")
    suspend fun deactivate(goalId: Long)

    @Query("SELECT * FROM blocked_apps WHERE goalId = :goalId")
    fun observeBlockedApps(goalId: Long): Flow<List<BlockedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedApp(app: BlockedApp): Long

    @Query("SELECT * FROM blocked_apps")
    fun observeAllBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps WHERE goalId = :goalId")
    suspend fun getBlockedApps(goalId: Long): List<BlockedApp>

    @Query("DELETE FROM blocked_apps WHERE goalId = :goalId")
    suspend fun clearBlockedApps(goalId: Long)

    @Query("DELETE FROM blocked_apps WHERE goalId = :goalId AND packageName = :packageName")
    suspend fun removeBlockedApp(goalId: Long, packageName: String)
}
