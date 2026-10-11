package com.demo.myapplication.domain.repository

import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    suspend fun addGoal(goal: Goal): Long
    suspend fun upsertBlockedApp(blockedApp: BlockedApp)
     fun getActiveGoals(): Flow<List<Goal>>
     fun observeBlockedApps(goalId:Long): Flow<List<BlockedApp>>
    suspend fun getGoalById(goalId:Long): Goal?

    // Keyed by Goal.id (not DailyGoal.id) so callers can look up "is today's
    // instance of this goal done" without caring about the DailyGoal row's
    // own identity - a goal with no DailyGoal row yet today just isn't
    // completed, same as one still PENDING.
    fun observeTodayDailyGoals(): Flow<List<DailyGoal>>
}