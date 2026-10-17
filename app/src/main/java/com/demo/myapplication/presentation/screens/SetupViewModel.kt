package com.demo.myapplication.presentation.screens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.myapplication.core.datastore.AppPreferences
import com.demo.myapplication.core.platform.InstalledAppsProvider
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.domain.usecase.AddGoalUseCase
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

enum class SetupStep { Name, Goals, Distractions, Permissions, Done }

/** A goal the user can pick in one tap during setup. */
@Immutable
data class GoalTemplate(
    val key: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val defaultSeconds: Long
)

/** A goal chosen during setup - a template or one the user typed. */
@Immutable
data class SetupGoal(
    val key: String,
    val name: String,
    val iconKey: String,
    val colorKey: String,
    val seconds: Long
)

@Immutable
data class SetupState(
    val step: SetupStep = SetupStep.Name,
    val name: String = "",
    val goals: List<SetupGoal> = emptyList(),
    val apps: List<AppOption> = emptyList(),
    val appsLoaded: Boolean = false,
    val blockedPackages: Set<String> = emptySet(),
    val isSaving: Boolean = false
) {
    // The steps actually shown: choosing apps only makes sense once there's
    // a goal to block them for.
    val steps: List<SetupStep>
        get() = SetupStep.entries.filter { it != SetupStep.Distractions || goals.isNotEmpty() }
    val stepIndex: Int get() = steps.indexOf(step).coerceAtLeast(0)
}

val GoalTemplates = listOf(
    GoalTemplate("study", "Study", "calculate", "teal", 60 * 60),
    GoalTemplate("reading", "Reading", "menu_book", "purple", 30 * 60),
    GoalTemplate("deep_work", "Deep work", "computer", "blue", 90 * 60),
    GoalTemplate("coding", "Coding", "code", "green", 60 * 60),
    GoalTemplate("writing", "Writing", "edit_note", "amber", 45 * 60),
    GoalTemplate("workout", "Workout", "fitness_center", "rose", 45 * 60),
    GoalTemplate("music", "Practice music", "music_note", "purple", 30 * 60),
    GoalTemplate("language", "A new language", "translate", "teal", 30 * 60),
    GoalTemplate("drawing", "Drawing", "brush", "amber", 30 * 60),
    GoalTemplate("meditation", "Meditation", "self_improvement", "green", 15 * 60)
)

val DurationPresets: List<Long> = listOf(15, 30, 45, 60, 90, 120).map { it * 60L }

/**
 * The apps people most often name as their distractions. Any that are
 * installed are pre-selected on the "What pulls you away?" step - a
 * starting point the user can untick, never a decision made for them.
 */
private val CommonDistractions = setOf(
    "com.instagram.android",
    "com.google.android.youtube",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.reddit.frontpage",
    "com.twitter.android",
    "com.facebook.katana",
    "com.snapchat.android",
    "com.netflix.mediaclient",
    "com.pinterest",
    "com.discord",
    "tv.twitch.android.app",
    "in.startv.hotstar",
    "com.amazon.avod.thirdpartyclient"
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val addGoalUseCase: AddGoalUseCase,
    private val installedAppsProvider: InstalledAppsProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SetupState())
    val state: StateFlow<SetupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Replaying the intro from Settings keeps the name already given.
            _state.update { it.copy(name = preferences.userName.first()) }
        }
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                installedAppsProvider.getLaunchableApps().map { app ->
                    AppOption(
                        appName = app.appName,
                        packageName = app.packageName,
                        icon = runCatching { app.icon.toBitmap().asImageBitmap() }.getOrNull()
                    )
                }
            }
            val suggested = apps.filter { it.packageName in CommonDistractions }.map { it.packageName }.toSet()
            _state.update {
                it.copy(
                    apps = apps.sortedByDescending { app -> app.packageName in suggested },
                    blockedPackages = suggested,
                    appsLoaded = true
                )
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name.take(30)) }

    fun toggleTemplate(template: GoalTemplate) = _state.update { s ->
        if (s.goals.any { it.key == template.key }) {
            s.copy(goals = s.goals.filterNot { it.key == template.key })
        } else {
            s.copy(
                goals = s.goals + SetupGoal(
                    key = template.key,
                    name = template.name,
                    iconKey = template.iconKey,
                    colorKey = template.colorKey,
                    seconds = template.defaultSeconds
                )
            )
        }
    }

    fun addCustomGoal(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _state.update { s ->
            val colorKey = com.demo.myapplication.presentation.theme.GoalVisuals.accentKeyFor(s.goals.size)
            s.copy(
                goals = s.goals + SetupGoal(
                    key = "custom_${System.nanoTime()}",
                    name = trimmed,
                    iconKey = com.demo.myapplication.presentation.theme.GoalVisuals.DefaultIcon,
                    colorKey = colorKey,
                    seconds = 60 * 60
                )
            )
        }
    }

    fun removeGoal(key: String) = _state.update { s -> s.copy(goals = s.goals.filterNot { it.key == key }) }

    fun setDuration(key: String, seconds: Long) = _state.update { s ->
        s.copy(goals = s.goals.map { if (it.key == key) it.copy(seconds = seconds) else it })
    }

    fun toggleApp(packageName: String) = _state.update { s ->
        val blocked = s.blockedPackages
        s.copy(blockedPackages = if (packageName in blocked) blocked - packageName else blocked + packageName)
    }

    fun next() = _state.update { s ->
        val steps = s.steps
        s.copy(step = steps.getOrElse(s.stepIndex + 1) { s.step })
    }

    /** False when already on the first step - the caller leaves setup instead. */
    fun back(): Boolean {
        val s = _state.value
        if (s.stepIndex == 0) return false
        _state.update { it.copy(step = s.steps[s.stepIndex - 1]) }
        return true
    }

    /**
     * Saves everything chosen in one go at the very end, so leaving setup
     * half-way never leaves stray goals behind.
     */
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.isSaving) return
        _state.update { it.copy(isSaving = true) }
        val appNames = s.apps.associate { it.packageName to it.appName }
        viewModelScope.launch {
            preferences.setUserName(s.name)
            s.goals.forEach { goal ->
                addGoalUseCase(
                    Goal(
                        name = goal.name,
                        dailyDurationSeconds = goal.seconds,
                        icon = goal.iconKey,
                        color = goal.colorKey
                    ),
                    s.blockedPackages.map { pkg ->
                        BlockedApp(goalId = 0L, packageName = pkg, appName = appNames[pkg] ?: pkg)
                    }
                )
            }
            preferences.setOnboardingComplete()
            onDone()
        }
    }
}
