package com.meltide.app.domain.usecase

import com.meltide.app.data.local.entity.DailyGoal
import com.meltide.app.data.local.entity.Goal
import com.meltide.app.domain.repository.GoalRepository
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
