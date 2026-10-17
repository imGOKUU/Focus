package com.demo.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.demo.myapplication.presentation.screens.AppViewModel
import com.demo.myapplication.presentation.screens.BlockableAppItem
import com.demo.myapplication.presentation.screens.FocusScreen
import com.demo.myapplication.presentation.screens.FocusViewModel
import com.demo.myapplication.presentation.screens.GoalConfigScreen
import com.demo.myapplication.presentation.screens.GoalEditorViewModel
import com.demo.myapplication.presentation.screens.HistoryScreen
import com.demo.myapplication.presentation.screens.SetupScreen
import com.demo.myapplication.presentation.screens.SetupViewModel
import com.demo.myapplication.presentation.screens.SettingsScreen
import com.demo.myapplication.presentation.screens.SettingsViewModel
import com.demo.myapplication.presentation.screens.HistoryViewModel
import com.demo.myapplication.presentation.screens.Routes
import com.demo.myapplication.presentation.screens.SessionResultScreen
import com.demo.myapplication.presentation.screens.TodayScreen
import com.demo.myapplication.presentation.screens.TodayViewModel
import com.demo.myapplication.presentation.screens.WelcomeScreen
import com.demo.myapplication.presentation.theme.GoalVisuals

private const val GoalEditorRoute = "goal_editor?goalId={goalId}"
private const val HistoryRoute = "history"
private const val FocusRoute = "focus_screen/{goalId}"
private const val ResultRoute = "session_result/{goalId}?completed={completed}&focused={focused}&remaining={remaining}"

private fun goalEditor(goalId: Long? = null) = "goal_editor?goalId=${goalId ?: -1L}"
private fun focus(goalId: Long) = "focus_screen/$goalId"

@Composable
fun NavScreen()
{
    val appViewModel: AppViewModel = hiltViewModel()
    val todayViewModel: TodayViewModel = hiltViewModel()
    val startDestination by appViewModel.startDestination.collectAsStateWithLifecycle()
    val today by todayViewModel.uiState.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val userName by appViewModel.userName.collectAsStateWithLifecycle()

    // Until DataStore says whether onboarding is done, show just the page
    // background - deciding the start screen up front avoids flashing the
    // intro at someone who's already been through it.
    val start = startDestination ?: run {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = start
    ){
        composable(Routes.Welcome){
            WelcomeScreen(
                onNavigate = { navController.navigate(Routes.Setup) }
            )
        }
        composable(Routes.Setup) {
            val setup: SetupViewModel = hiltViewModel()
            val setupState by setup.state.collectAsStateWithLifecycle()
            val leave: () -> Unit = { if (!setup.back()) navController.popBackStack() }
            BackHandler(onBack = leave)
            SetupScreen(
                state = setupState,
                onNameChange = setup::onNameChange,
                onToggleTemplate = setup::toggleTemplate,
                onAddCustomGoal = setup::addCustomGoal,
                onRemoveGoal = setup::removeGoal,
                onSetDuration = setup::setDuration,
                onToggleApp = setup::toggleApp,
                onNext = setup::next,
                onBack = leave,
                onFinish = {
                    setup.finish {
                        // Clear the whole intro (also when replayed from Settings).
                        navController.navigate(Routes.Today) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Routes.Settings) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val current by settingsViewModel.settings.collectAsStateWithLifecycle()
            SettingsScreen(
                settings = current,
                onBack = { navController.popBackStack() },
                onSessionNotificationChange = settingsViewModel::setSessionNotification,
                onCompletionAlertChange = settingsViewModel::setCompletionAlert,
                onStartInMinimalChange = settingsViewModel::setStartInMinimalMode,
                onKeepScreenOnChange = settingsViewModel::setKeepScreenOn,
                onReplayIntro = { navController.navigate(Routes.Welcome) },
                userName = userName,
                onUserNameChange = appViewModel::setUserName
            )
        }
        composable(Routes.Today){
            TodayScreen(
                state = today,
                userName = userName,
                onStartGoal = { goal -> navController.navigate(focus(goal.id)) },
                onEditGoal = { goal -> navController.navigate(goalEditor(goal.id)) },
                onAddGoal = { navController.navigate(goalEditor()) },
                onOpenHistory = { navController.navigate(HistoryRoute) },
                onOpenSettings = { navController.navigate(Routes.Settings) }
            )
        }
        composable(HistoryRoute) {
            val history: HistoryViewModel = hiltViewModel()
            val state by history.uiState.collectAsStateWithLifecycle()
            HistoryScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onSelectDate = history::selectDate,
                onPreviousMonth = history::showPreviousMonth,
                onNextMonth = history::showNextMonth
            )
        }
        composable(
            GoalEditorRoute,
            arguments = listOf(navArgument("goalId") { type = NavType.LongType; defaultValue = -1L })
        ){
            val editor: GoalEditorViewModel = hiltViewModel()
            val form by editor.state.collectAsStateWithLifecycle()
            if (!form.isLoaded) {
                // The wheels read their initial value once, so wait for the
                // saved goal before composing the form.
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                return@composable
            }
            GoalConfigScreen(
                goalName = form.name,
                onGoalNameChange = editor::onNameChange,
                onBack = { navController.popBackStack() },
                selectedHours = form.hours,
                onHoursChange = editor::onHoursChange,
                selectedMinutes = form.minutes,
                onMinutesChange = editor::onMinutesChange,
                blockableApps = form.apps.map { app ->
                    BlockableAppItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        icon = app.icon,
                        isBlocked = app.packageName in form.blockedPackages
                    )
                },
                onToggleApp = { editor.onToggleApp(it.packageName) },
                isEditing = form.isEditing,
                iconKey = form.iconKey,
                colorKey = form.colorKey,
                onIconChange = editor::onIconChange,
                onColorChange = editor::onColorChange,
                onDelete = { editor.delete { navController.popBackStack() } },
                onSave = { editor.save { navController.popBackStack() } }
            )
        }
        composable(
            FocusRoute,
            arguments = listOf(
                navArgument("goalId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val focusViewModel: FocusViewModel = hiltViewModel()
            val focusUiState = focusViewModel.uiState.collectAsStateWithLifecycle()
            val goalId =
                backStackEntry.arguments?.getLong("goalId")
            val goal = today.goals.find { it.id == goalId }
            LaunchedEffect(goalId) {
                goalId?.let { focusViewModel.start(it) }
            }
            // Pause first: it reads the current session synchronously, then
            // finishes in the app scope even after this screen's ViewModel is
            // cleared by the pop.
            val leaveFocus: () -> Unit = {
                focusViewModel.pause()
                navController.popBackStack()
            }
            // System back (button or gesture) must behave like the on-screen
            // back arrow - otherwise the session keeps running unnoticed.
            BackHandler(onBack = leaveFocus)
            LaunchedEffect(focusUiState.value.outcome) {
                val outcome = focusUiState.value.outcome ?: return@LaunchedEffect
                navController.navigate(
                    "session_result/$goalId?completed=${outcome.goalCompleted}" +
                        "&focused=${outcome.focusedSeconds}&remaining=${outcome.remainingTodaySeconds}"
                ) {
                    popUpTo(FocusRoute) { inclusive = true }
                }
            }
            LaunchedEffect(focusUiState.value.isUnavailable) {
                if (focusUiState.value.isUnavailable) navController.popBackStack()
            }
            FocusScreen(
                goal =goal,
                remainingSeconds =focusUiState.value.remainingSeconds ,
                totalSeconds = focusUiState.value.totalSeconds,
                blockedAppsCount = focusUiState.value.blockedApps,
                accent = goal?.accent ?: GoalVisuals.accent(""),
                isPaused = focusUiState.value.isPaused,
                onPause = {
                    focusViewModel.pause()
                },
                onResume = {
                    focusViewModel.resume()
                },
                onEndSession = { focusViewModel.endSession() },
                startInMinimalMode = settings.startInMinimalMode,
                keepScreenOn = settings.keepScreenOn,
                onBack = leaveFocus
            )
        }
        composable(
            ResultRoute,
            arguments = listOf(
                navArgument("goalId") { type = NavType.LongType },
                navArgument("completed") { type = NavType.BoolType; defaultValue = false },
                navArgument("focused") { type = NavType.LongType; defaultValue = 0L },
                navArgument("remaining") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { entry ->
            val args = entry.arguments
            val goal = today.goals.find { it.id == args?.getLong("goalId") }
            SessionResultScreen(
                goalName = goal?.name.orEmpty(),
                completed = args?.getBoolean("completed") ?: false,
                focusedSeconds = args?.getLong("focused") ?: 0L,
                remainingTodaySeconds = args?.getLong("remaining") ?: 0L,
                accent = goal?.accent ?: GoalVisuals.accent(""),
                onClose = { navController.popBackStack() }
            )
        }
    }
}
