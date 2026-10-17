package com.meltide.app.presentation.screens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meltide.app.core.platform.InstalledAppsProvider
import com.meltide.app.data.local.entity.BlockedApp
import com.meltide.app.data.local.entity.Goal
import com.meltide.app.domain.usecase.AddGoalUseCase
import com.meltide.app.domain.usecase.GetGoalByIdUseCase
import com.meltide.app.domain.usecase.GetGoalsUseCase
import com.meltide.app.domain.usecase.SessionUsecase
import com.meltide.app.presentation.theme.GoalVisuals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class AppOption(val appName: String, val packageName: String, val icon: ImageBitmap?)

@Immutable
data class GoalEditorState(
    val isLoaded: Boolean = false,
    val goalId: Long? = null,
    val name: String = "",
    val hours: Int = 1,
    val minutes: Int = 0,
    val iconKey: String = GoalVisuals.DefaultIcon,
    val colorKey: String = GoalVisuals.accents.first().first,
    val apps: List<AppOption> = emptyList(),
    val blockedPackages: Set<String> = emptySet()
) {
    val isEditing: Boolean get() = goalId != null
}

/**
 * Backs both "new goal" and "edit goal" (goalId nav argument present). Holds
 * the form in the ViewModel so it survives rotation and tablet resizing.
 */
@HiltViewModel
class GoalEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addGoalUseCase: AddGoalUseCase,
    private val getGoalByIdUseCase: GetGoalByIdUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val sessionUsecase: SessionUsecase,
    private val installedAppsProvider: InstalledAppsProvider
) : ViewModel() {

    private val goalId: Long? = savedStateHandle.get<Long>("goalId")?.takeIf { it > 0 }

    private val _state = MutableStateFlow(GoalEditorState())
    val state: StateFlow<GoalEditorState> = _state.asStateFlow()

    private var existingGoal: Goal? = null
    // Names of saved blocked apps, kept so an app that's since been
    // uninstalled doesn't silently drop off the goal when it's re-saved.
    private var savedAppNames: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) {
                installedAppsProvider.getLaunchableApps().map { app ->
                    AppOption(
                        appName = app.appName,
                        packageName = app.packageName,
                        icon = runCatching { app.icon.toBitmap().asImageBitmap() }.getOrNull()
                    )
                }
            }
            val goal = goalId?.let { getGoalByIdUseCase(it) }
            existingGoal = goal
            val blocked = goal?.let { getGoalsUseCase.getBlockedApps(it.id) }.orEmpty()
            savedAppNames = blocked.associate { it.packageName to it.appName }
            val blockedSet = savedAppNames.keys

            _state.value = if (goal != null) {
                GoalEditorState(
                    goalId = goal.id,
                    name = goal.name,
                    hours = (goal.dailyDurationSeconds / 3600).toInt(),
                    minutes = ((goal.dailyDurationSeconds % 3600) / 60).toInt(),
                    iconKey = goal.icon,
                    colorKey = goal.color
                )
            } else {
                GoalEditorState(colorKey = GoalVisuals.accentKeyFor(getGoalsUseCase().first().size))
            }.copy(
                // Already-blocked apps first, fixed for the life of the screen -
                // re-sorting on every toggle would make rows jump under the finger.
                apps = installed.sortedByDescending { it.packageName in blockedSet },
                blockedPackages = blockedSet,
                isLoaded = true
            )
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onHoursChange(hours: Int) = _state.update { it.copy(hours = hours) }
    fun onMinutesChange(minutes: Int) = _state.update { it.copy(minutes = minutes) }
    fun onIconChange(key: String) = _state.update { it.copy(iconKey = key) }
    fun onColorChange(key: String) = _state.update { it.copy(colorKey = key) }

    fun onToggleApp(packageName: String) = _state.update {
        val blocked = it.blockedPackages
        it.copy(blockedPackages = if (packageName in blocked) blocked - packageName else blocked + packageName)
    }

    fun save(onDone: () -> Unit) {
        val form = _state.value
        val appNames = savedAppNames + form.apps.associate { it.packageName to it.appName }
        val blockedApps = form.blockedPackages.map { pkg ->
            BlockedApp(goalId = form.goalId ?: 0L, packageName = pkg, appName = appNames[pkg] ?: pkg)
        }
        val duration = form.hours * 3600L + form.minutes * 60L
        viewModelScope.launch {
            val existing = existingGoal
            if (existing != null) {
                addGoalUseCase.update(
                    existing.copy(
                        name = form.name.trim(),
                        dailyDurationSeconds = duration,
                        icon = form.iconKey,
                        color = form.colorKey
                    ),
                    blockedApps
                )
            } else {
                addGoalUseCase(
                    Goal(name = form.name.trim(), dailyDurationSeconds = duration, icon = form.iconKey, color = form.colorKey),
                    blockedApps
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = goalId ?: return
        viewModelScope.launch {
            // A running session for a deleted goal would keep blocking apps.
            sessionUsecase.endActiveSessionForGoal(id)
            addGoalUseCase.delete(id)
            onDone()
        }
    }
}
