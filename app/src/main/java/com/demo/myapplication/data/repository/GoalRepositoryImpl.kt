package com.demo.myapplication.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.GoalDao
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val dailyGoalDao: DailyGoalDao,
    private val timeProvider: TimeProvider
) : GoalRepository {
    override suspend fun addGoal(goal: Goal): Long {
        return goalDao.upsert(goal = goal)
    }

    override suspend fun upsertBlockedApp(blockedApp: BlockedApp) {
        goalDao.upsertBlockedApp(blockedApp)
    }
    override  fun getActiveGoals(): Flow<List<Goal>> {
        return goalDao.observeActiveGoals()
    }
    override  fun observeBlockedApps(goalId:Long): Flow<List<BlockedApp>> {
        return goalDao.observeBlockedApps(goalId = goalId)
    }
   suspend override  fun getGoalById(goalId:Long): Goal? {
        return goalDao.getGoalById(goalId = goalId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun observeTodayDailyGoals(): Flow<List<DailyGoal>> {
        val today = timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate()
        return dailyGoalDao.observeForDate(today)
    }
}