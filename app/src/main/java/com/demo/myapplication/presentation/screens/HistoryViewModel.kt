package com.demo.myapplication.presentation.screens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.entity.GoalStatus
import com.demo.myapplication.domain.usecase.GetHistoryUseCase
import com.demo.myapplication.domain.usecase.SessionUsecase
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalVisuals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/** One goal's record on one day. */
@Immutable
data class DayGoalEntry(
    val goalId: Long,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val plannedSeconds: Long,
    val givenSeconds: Long,
    val isDone: Boolean
) {
    val icon: ImageVector get() = GoalVisuals.icon(iconKey)
    val accent: GoalAccent get() = GoalVisuals.accent(colorKey)
}

@Immutable
data class DaySummary(val date: LocalDate, val entries: List<DayGoalEntry>) {
    val givenSeconds: Long get() = entries.sumOf { it.givenSeconds }
    val plannedSeconds: Long get() = entries.sumOf { it.plannedSeconds }
    val fraction: Float get() = if (plannedSeconds == 0L) 0f else (givenSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f)
}

/** A goal's total across the shown month. */
@Immutable
data class GoalMonthTotal(
    val goalId: Long,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val givenSeconds: Long,
    val daysDone: Int
) {
    val icon: ImageVector get() = GoalVisuals.icon(iconKey)
    val accent: GoalAccent get() = GoalVisuals.accent(colorKey)
}

@Immutable
data class HistoryUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val days: Map<LocalDate, DaySummary> = emptyMap(),
    val goalTotals: List<GoalMonthTotal> = emptyList(),
    val isLoaded: Boolean = false
) {
    val selectedDay: DaySummary? get() = days[selectedDate]
    val monthGivenSeconds: Long get() = days.values.sumOf { it.givenSeconds }
    val activeDayCount: Int get() = days.values.count { it.givenSeconds > 0 }
    val canGoForward: Boolean get() = month < YearMonth.from(today)
}

/**
 * Month-at-a-time history (spec: calendar dots, per-goal totals, tap a date
 * for detail). Only facts - time given vs planned - no scores or streaks.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistoryUseCase: GetHistoryUseCase,
    sessionUsecase: SessionUsecase,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val today: LocalDate get() = timeProvider.now().atZone(ZoneId.systemDefault()).toLocalDate()

    private val month = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow(today)

    // A running session's time isn't written to its DailyGoal until it ends,
    // so today's figures add it live; refreshing every 30s is plenty here.
    private val clock = flow {
        while (true) {
            emit(timeProvider.now())
            delay(30.seconds)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthDailyGoals = month.flatMapLatest { ym ->
        getHistoryUseCase.observeDailyGoals(ym.atDay(1), ym.atEndOfMonth())
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        combine(month, selectedDate, ::Pair),
        monthDailyGoals,
        getHistoryUseCase.observeAllGoals(),
        sessionUsecase.observeActiveSession(),
        clock
    ) { (ym, selected), dailyGoals, goals, activeSession, now ->
        val goalsById = goals.associateBy { it.id }
        val days = dailyGoals
            .groupBy { it.date }
            .mapValues { (date, records) ->
                DaySummary(
                    date = date,
                    entries = records.mapNotNull { daily ->
                        val goal = goalsById[daily.goalId] ?: return@mapNotNull null
                        val live = activeSession
                            ?.takeIf { it.dailyGoalId == daily.id }
                            ?.let { it.allocatedSeconds - timeProvider.remainingSeconds(it, now) }
                            ?: 0L
                        DayGoalEntry(
                            goalId = goal.id,
                            name = goal.name,
                            iconKey = goal.icon,
                            colorKey = goal.color,
                            plannedSeconds = daily.allocatedSeconds,
                            givenSeconds = (daily.completedSeconds + live).coerceAtMost(daily.allocatedSeconds),
                            isDone = daily.status == GoalStatus.COMPLETED
                        )
                    }
                )
            }
            .filterValues { it.entries.isNotEmpty() }

        val totals = days.values
            .flatMap { it.entries }
            .groupBy { it.goalId }
            .map { (goalId, entries) ->
                val first = entries.first()
                GoalMonthTotal(
                    goalId = goalId,
                    name = first.name,
                    iconKey = first.iconKey,
                    colorKey = first.colorKey,
                    givenSeconds = entries.sumOf { it.givenSeconds },
                    daysDone = entries.count { it.isDone }
                )
            }
            .filter { it.givenSeconds > 0 }
            .sortedByDescending { it.givenSeconds }

        HistoryUiState(
            month = ym,
            today = today,
            selectedDate = selected,
            days = days,
            goalTotals = totals,
            isLoaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectDate(date: LocalDate) {
        if (date <= today) selectedDate.value = date
    }

    fun showPreviousMonth() = changeMonth(-1)

    fun showNextMonth() {
        if (month.value < YearMonth.from(today)) changeMonth(1)
    }

    // Moving months selects that month's last day you could have used -
    // today in the current month, otherwise the month's final day.
    private fun changeMonth(delta: Long) {
        month.update { it.plusMonths(delta) }
        val ym = month.value
        selectedDate.value = if (ym == YearMonth.from(today)) today else ym.atEndOfMonth()
    }
}
