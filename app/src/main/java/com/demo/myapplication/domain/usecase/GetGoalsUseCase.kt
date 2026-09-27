package com.demo.myapplication.domain.usecase

import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.repository.GoalRepository
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
}