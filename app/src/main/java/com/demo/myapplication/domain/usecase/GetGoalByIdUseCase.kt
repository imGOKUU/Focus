package com.demo.myapplication.domain.usecase

import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.repository.GoalRepository
import javax.inject.Inject

class GetGoalByIdUseCase @Inject constructor(
   private val repository: GoalRepository
){
    suspend operator fun invoke(goalId: Long): Goal? {
        return repository.getGoalById(goalId)
    }
}