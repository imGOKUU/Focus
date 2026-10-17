package com.demo.myapplication.domain.repository

import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface GoalRepository {
    suspend fun addGoal(goal: Goal): Long
    // Inserts the goal and its blocked apps in one transaction - all or nothing.
    suspend fun addGoalWithBlockedApps(goal: Goal, blockedApps: List<BlockedApp>): Long
    suspend fun upsertBlockedApp(blockedApp: BlockedApp)
     fun getActiveGoals(): Flow<List<Goal>>
     fun observeBlockedApps(goalId:Long): Flow<List<BlockedApp>>
    suspend fun getGoalById(goalId:Long): Goal?
    suspend fun getBlockedApps(goalId: Long): List<BlockedApp>
    fun observeAllBlockedApps(): Flow<List<BlockedApp>>

    // Replaces the goal's fields and its whole block list in one transaction,
    // and carries a new daily duration over to today's (unfinished) instance.
    suspend fun updateGoalWithBlockedApps(goal: Goal, blockedApps: List<BlockedApp>)

    // Soft delete - history stays (spec section 4).
    suspend fun deleteGoal(goalId: Long)

    // Keyed by Goal.id (not DailyGoal.id) so callers can look up "is today's
    // instance of this goal done" without caring about the DailyGoal row's
    // own identity - a goal with no DailyGoal row yet today just isn't
    // completed, same as one still PENDING.
    fun observeTodayDailyGoals(): Flow<List<DailyGoal>>

    // History: every goal ever made (deleted ones too), and daily records in a date range.
    fun observeAllGoals(): Flow<List<Goal>>
    fun observeDailyGoalsBetween(start: LocalDate, end: LocalDate): Flow<List<DailyGoal>>
}