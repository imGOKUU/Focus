package com.demo.myapplication.domain.usecase

import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: GoalRepository
) {
    fun observeAllGoals(): Flow<List<Goal>> = repository.observeAllGoals()

    fun observeDailyGoals(start: LocalDate, end: LocalDate): Flow<List<DailyGoal>> =
        repository.observeDailyGoalsBetween(start, end)
}
