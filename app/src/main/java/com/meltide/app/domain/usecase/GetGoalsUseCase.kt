package com.meltide.app.domain.usecase

import com.meltide.app.data.local.entity.BlockedApp
import com.meltide.app.data.local.entity.DailyGoal
import com.meltide.app.data.local.entity.Goal
import com.meltide.app.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoalsUseCase @Inject constructor(
    private val repository: GoalRepository
) {
     operator fun invoke(): Flow<List<Goal>> {
        return repository.getActiveGoals()
    }
     fun observeBlockedApps(goalId:Long):Flow<List<BlockedApp>>{
       return repository.observeBlockedApps(goalId)
    }

    fun observeAllBlockedApps(): Flow<List<BlockedApp>> = repository.observeAllBlockedApps()

    suspend fun getBlockedApps(goalId: Long): List<BlockedApp> = repository.getBlockedApps(goalId)

    fun observeTodayDailyGoals(): Flow<List<DailyGoal>> {
        return repository.observeTodayDailyGoals()
    }
}