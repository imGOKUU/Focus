package com.demo.myapplication.presentation.screens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.entity.GoalStatus
import com.demo.myapplication.data.local.entity.SessionStatus
import com.demo.myapplication.domain.usecase.GetGoalsUseCase
import com.demo.myapplication.domain.usecase.SessionUsecase
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalVisuals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Immutable
data class BlockedAppInfo(val packageName: String, val appName: String)

enum class GoalProgress { NotStarted, Partial, Running, Paused, Done }

@Immutable
data class GoalListItem(
    val id: Long,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val dailySeconds: Long,
    // What's left of today's time - counts down live while a session runs.
    val remainingSeconds: Long,
    val progress: GoalProgress,
    val blockedApps: List<BlockedAppInfo>
) {
    val icon: ImageVector get() = GoalVisuals.icon(iconKey)
    val accent: GoalAccent get() = GoalVisuals.accent(colorKey)
    val isCompletedToday: Boolean get() = progress == GoalProgress.Done
    val hasActiveSession: Boolean get() = progress == GoalProgress.Running || progress == GoalProgress.Paused
}

@Immutable
data class TodayUiState(
    val goals: List<GoalListItem> = emptyList(),
    val totalSeconds: Long = 0L,
    val remainingSeconds: Long = 0L,
    val isLoaded: Boolean = false
) {
    val doneCount: Int get() = goals.count { it.isCompletedToday }
    val activeGoal: GoalListItem? get() = goals.firstOrNull { it.hasActiveSession }
    // What the main button offers: the running goal first, else the next one not done.
    val nextGoal: GoalListItem? get() = activeGoal ?: goals.firstOrNull { !it.isCompletedToday }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    getGoalsUseCase: GetGoalsUseCase,
    sessionUsecase: SessionUsecase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    // Drives the live countdown of a running goal on the Today list. When
    // nothing is running every tick produces an equal state, which StateFlow
    // drops, so idle ticks never reach the UI.
    private val clock = flow {
        while (true) {
            emit(timeProvider.now())
            delay(1.seconds)
        }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        getGoalsUseCase(),
        getGoalsUseCase.observeTodayDailyGoals(),
        getGoalsUseCase.observeAllBlockedApps(),
        sessionUsecase.observeActiveSession(),
        clock
    ) { goals, todayDailyGoals, blockedApps, activeSession, now ->
        val dailyByGoal = todayDailyGoals.associateBy { it.goalId }
        val blockedByGoal = blockedApps.groupBy { it.goalId }
        val items = goals.map { goal ->
            val daily = dailyByGoal[goal.id]
            val allocated = daily?.allocatedSeconds ?: goal.dailyDurationSeconds
            val completed = daily?.completedSeconds ?: 0L
            val session = activeSession?.takeIf { daily != null && it.dailyGoalId == daily.id }
            val (remaining, progress) = when {
                daily?.status == GoalStatus.COMPLETED -> 0L to GoalProgress.Done
                session != null -> timeProvider.remainingSeconds(session, now) to
                    if (session.status == SessionStatus.PAUSED) GoalProgress.Paused else GoalProgress.Running
                completed > 0 -> (allocated - completed).coerceAtLeast(0L) to GoalProgress.Partial
                else -> allocated to GoalProgress.NotStarted
            }
            GoalListItem(
                id = goal.id,
                name = goal.name,
                iconKey = goal.icon,
                colorKey = goal.color,
                dailySeconds = allocated,
                remainingSeconds = remaining,
                progress = progress,
                blockedApps = blockedByGoal[goal.id].orEmpty()
                    .map { BlockedAppInfo(packageName = it.packageName, appName = it.appName) }
                    .sortedBy { it.appName.lowercase() }
            )
        }
        TodayUiState(
            goals = items,
            totalSeconds = items.sumOf { it.dailySeconds },
            remainingSeconds = items.sumOf { it.remainingSeconds },
            isLoaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())
}
