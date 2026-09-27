package com.demo.myapplication.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.myapplication.core.platform.InstalledApp
import com.demo.myapplication.core.platform.InstalledAppsProvider
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.usecase.AddGoalUseCase
import com.demo.myapplication.domain.usecase.GetGoalsUseCase
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalAccents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.String

@HiltViewModel
class WelcomeViewModel @Inject constructor (
private val addGoalUseCase: AddGoalUseCase,
private val getGoalsUseCase: GetGoalsUseCase,
private val installedAppsProvider: InstalledAppsProvider
): ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()


    init {
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) {
                installedAppsProvider.getLaunchableApps()
            }
        }
    }

    fun createGoal(goal: Goal, blockedApp: List<BlockedApp>) {
        viewModelScope.launch {
            val goalId = addGoalUseCase.invoke(goal)
            blockedApp.forEach {
                addGoalUseCase.upsertBlockedApp(it.copy(goalId = goalId))
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val goalList: StateFlow<List<GoalListItem>> = getGoalsUseCase().map{ goals ->
            goals.map {goal->
                GoalListItem(
                    name = goal.name,
                    accent = goal.color.toGoalAccent(),
                    durationLabel = goal.dailyDurationSeconds.toString(),
                    icon = goal.icon.toGoalIcon(),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),emptyList())

    private fun String.toGoalAccent(): GoalAccent = when (this) {
        "teal" -> GoalAccents.Teal
        "blue" -> GoalAccents.Blue
        "green" -> GoalAccents.Green
        "purple" -> GoalAccents.Purple
        else -> GoalAccents.Teal
    }

    private fun String.toGoalIcon(): ImageVector = when (this) {
        "bar_chart" -> Icons.Outlined.BarChart
        "code" -> Icons.Outlined.Code
        "computer" -> Icons.Outlined.Computer
        "menu_book" -> Icons.Outlined.MenuBook
        else -> Icons.Outlined.BarChart
    }



}