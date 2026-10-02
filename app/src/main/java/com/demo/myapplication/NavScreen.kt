package com.demo.myapplication

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.demo.myapplication.presentation.screens.AddGoalsScreen
import com.demo.myapplication.presentation.screens.BlockableAppItem
import com.demo.myapplication.presentation.screens.FocusScreen
import com.demo.myapplication.presentation.screens.GoalConfigScreen
import com.demo.myapplication.presentation.screens.WelcomeScreen
import com.demo.myapplication.presentation.screens.WelcomeViewModel
import kotlinx.coroutines.flow.map

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
            AddGoalsScreen(
                goals = goals.value,
                onBack = { navController.popBackStack() },
                onGoalClick = { goal->
                   navController.navigate("focus_screen/${Uri.encode(goal.id)}")
                },
                onAddGoal =
                {
                    navController.navigate("add_goal")
                }
            )
        }
        composable("add_goal"){
            var goalName by remember { mutableStateOf("") }
            var selectedHours by remember { mutableStateOf(1) }
            var selectedMinutes by remember { mutableStateOf(0) }
            val installedApps by goalViewModel.installedApps.collectAsStateWithLifecycle()
            var blockableApps by remember { mutableStateOf<List<BlockableAppItem>>(emptyList()) }
            LaunchedEffect(installedApps) {
                if (installedApps.isNotEmpty()) {
                    blockableApps = installedApps.map { app ->
                        BlockableAppItem(
                            appName = app.appName,
                            packageName = app.packageName,
                            icon = runCatching { app.icon.toBitmap().asImageBitmap() }.getOrNull(),
                            isBlocked = false
                        )
                    }
                }
            }

            GoalConfigScreen(
                goalName = goalName,
                onGoalNameChange = { goalName = it },
                onBack = { navController.popBackStack() },
                selectedHours = selectedHours,
                onHoursChange = { selectedHours = it },
                selectedMinutes = selectedMinutes,
                onMinutesChange = { selectedMinutes = it },
                blockableApps = blockableApps,
                onToggleApp = { toggled ->
                    blockableApps = blockableApps.map { app ->
                        if (app.packageName == toggled.packageName) {
                            app.copy(isBlocked = !app.isBlocked)
                        } else {
                            app
                        }
                    }
                },
                onSave = {
                    val goal = Goal(
                        name = goalName,
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
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val goalId =
                backStackEntry.arguments?.getString("goalId") ?: ""
 val goal =  goals.value.find { it.id==goalId
 }
            FocusScreen(
                goal =goal,
                onPause = {},
                onBack = { navController.popBackStack() }
            )
        }

    }
}