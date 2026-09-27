package com.demo.myapplication.domain.usecase

import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.repository.GoalRepository
import javax.inject.Inject

class AddGoalUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goal: Goal): Long {
        return repository.addGoal(goal)
    }
    suspend fun upsertBlockedApp(blockedApp:BlockedApp){
        repository.upsertBlockedApp(blockedApp)
    }
}