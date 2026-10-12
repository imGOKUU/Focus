package com.demo.myapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.demo.myapplication.data.local.entity.BlockedApp
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.presentation.screens.HomeScreen
import com.demo.myapplication.presentation.screens.BlockableAppItem
import com.demo.myapplication.presentation.screens.FocusScreen
import com.demo.myapplication.presentation.screens.FocusViewModel
import com.demo.myapplication.presentation.screens.GoalConfigScreen
import com.demo.myapplication.presentation.screens.WelcomeScreen
import com.demo.myapplication.presentation.screens.WelcomeViewModel

@Composable
fun NavScreen()
{
  val goalViewModel: WelcomeViewModel = hiltViewModel()
    val navController = rememberNavController()

    val goals = goalViewModel.goalList.collectAsStateWithLifecycle()

    NavHost(
       navController = navController,
        startDestination = "landing_page"
    ){
        composable("landing_page"){
            WelcomeScreen(
                onNavigate = { navController.navigate("home_page") }
            )
        }
        composable("home_page"){
            HomeScreen(
                goals = goals.value,
                onBack = { navController.popBackStack() },
                onGoalClick = { goal->
                   navController.navigate("focus_screen/${goal.id}")

                },
                onAddGoal =
                {
                    navController.navigate("add_goal")
                }
            )
        }
        composable("add_goal"){
            // rememberSaveable so the form survives rotation / tablet resizing.
            var goalName by rememberSaveable { mutableStateOf("") }
            var selectedHours by rememberSaveable { mutableStateOf(1) }
            var selectedMinutes by rememberSaveable { mutableStateOf(0) }
            val installedApps by goalViewModel.installedApps.collectAsStateWithLifecycle()
            // Only the chosen package names are saved; icons (bitmaps) can't go
            // in saved state, so they're rebuilt from installedApps instead.
            var blockedPackages by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
            val appIcons = remember(installedApps) {
                installedApps.associate { app ->
                    app.packageName to runCatching { app.icon.toBitmap().asImageBitmap() }.getOrNull()
                }
            }
            val blockableApps = installedApps.map { app ->
                BlockableAppItem(
                    appName = app.appName,
                    packageName = app.packageName,
                    icon = appIcons[app.packageName],
                    isBlocked = app.packageName in blockedPackages
                )
            }
            GoalConfigScreen(
                goalName = goalName,
                onGoalNameChange = { goalName = it },
                onBack = {
                    println("Back clicked")
                    navController.popBackStack()
                         },
                selectedHours = selectedHours,
                onHoursChange = { selectedHours = it },
                selectedMinutes = selectedMinutes,
                onMinutesChange = { selectedMinutes = it },
                blockableApps = blockableApps,
                onToggleApp = { toggled ->
                    blockedPackages = ArrayList(
                        if (toggled.packageName in blockedPackages) {
                            blockedPackages - toggled.packageName
                        } else {
                            blockedPackages + toggled.packageName
                        }
                    )
                },
                onSave = {
                    val goal = Goal(
                        name = goalName.trim(),
                        dailyDurationSeconds = (selectedHours * 3600L) + (selectedMinutes * 60L),
                        icon = "default",
                        color = "teal"
                    )
                    val blockedApps = blockableApps
                        .filter { it.isBlocked }
                        .map { BlockedApp(goalId = 0L, packageName = it.packageName, appName = it.appName) }

                    goalViewModel.createGoal(goal, blockedApps)
                    navController.popBackStack()
                }
            )
        }
        composable(
            "focus_screen/{goalId}",
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
 val goal =  goals.value.find { it.id==goalId
 }
            LaunchedEffect(goalId) {
                goalId?.let { focusViewModel.start(it) }
            }
            LaunchedEffect(focusUiState.value.isCompleted) {
                if (focusUiState.value.isCompleted) {
                    navController.popBackStack()
                }
            }
            FocusScreen(
                goal =goal,
                remainingSeconds =focusUiState.value.remainingSeconds ,
                totalSeconds = focusUiState.value.totalSeconds,
                blockedAppsCount = focusUiState.value.blockedApps,
                isPaused = focusUiState.value.isPaused,
                onPause = {
                    focusViewModel.pause()
                },
                onResume = {
                    focusViewModel.resume()
                },
                onBack = {
                    // Pause first: it reads the current session synchronously,
                    // then finishes in the app scope even after this screen's
                    // ViewModel is cleared by the pop.
                    focusViewModel.pause()
                    navController.popBackStack()
                }
            )
        }

    }
}