package com.demo.myapplication.domain.usecase

import android.os.Build
import androidx.annotation.RequiresApi
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.DailyGoal
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun observeTodayDailyGoals(): Flow<List<DailyGoal>> {
        return repository.observeTodayDailyGoals()
    }
}