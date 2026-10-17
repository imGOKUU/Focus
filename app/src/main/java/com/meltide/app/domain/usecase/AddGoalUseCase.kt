package com.meltide.app.domain.usecase

import com.meltide.app.data.local.entity.BlockedApp
import com.meltide.app.data.local.entity.Goal
import com.meltide.app.domain.repository.GoalRepository
import javax.inject.Inject

class AddGoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goal: Goal): Long {
        return repository.addGoal(goal)
    }
    suspend operator fun invoke(goal: Goal, blockedApps: List<BlockedApp>): Long {
        return repository.addGoalWithBlockedApps(goal, blockedApps)
    }
    suspend fun update(goal: Goal, blockedApps: List<BlockedApp>) =
        repository.updateGoalWithBlockedApps(goal, blockedApps)

    suspend fun delete(goalId: Long) = repository.deleteGoal(goalId)

    suspend fun upsertBlockedApp(blockedApp:BlockedApp){
        repository.upsertBlockedApp(blockedApp)
    }
}