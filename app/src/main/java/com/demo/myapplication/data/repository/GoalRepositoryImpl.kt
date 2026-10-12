package com.demo.myapplication.data.repository

import androidx.room.withTransaction
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.FocusDatabase
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.GoalDao
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.repository.GoalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class GoalRepositoryImpl @Inject constructor(
    private val database: FocusDatabase,
    private val goalDao: GoalDao,
    private val dailyGoalDao: DailyGoalDao,
    private val timeProvider: TimeProvider
) : GoalRepository {
    override suspend fun addGoal(goal: Goal): Long {
        return goalDao.upsert(goal = goal)
    }

    override suspend fun addGoalWithBlockedApps(goal: Goal, blockedApps: List<BlockedApp>): Long =
        database.withTransaction {
            val goalId = goalDao.upsert(goal = goal)
            blockedApps.forEach { goalDao.upsertBlockedApp(it.copy(goalId = goalId)) }
            goalId
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

    // "Today" is re-checked every minute rather than fixed when the flow is
    // created, so the list rolls over at midnight while the app stays open.
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTodayDailyGoals(): Flow<List<DailyGoal>> =
        flow {
            while (true) {
                emit(timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate())
                delay(60.seconds)
            }
        }
            .distinctUntilChanged()
            .flatMapLatest { today -> dailyGoalDao.observeForDate(today) }
}