package com.meltide.app.domain.usecase

import com.meltide.app.data.local.entity.Goal
import com.meltide.app.domain.repository.GoalRepository
import javax.inject.Inject

class GetGoalByIdUseCase @Inject constructor(
   private val repository: GoalRepository
){
    suspend operator fun invoke(goalId: Long): Goal? {
        return repository.getGoalById(goalId)
    }
}