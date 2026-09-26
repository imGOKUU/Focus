package com.demo.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demo.myapplication.ui.screens.LandingPage
import com.demo.myapplication.ui.screens.WelcomeScreen

@Composable
fun NavScreen()
{
    val navController = rememberNavController()
    NavHost(
       navController = navController,
        startDestination = "landing_page"
    ){
        composable("landing_page"){
            LandingPage(
                onNavigate = { navController.navigate("home_page") }
            )
        }
        composable("home_page"){
            WelcomeScreen()
        }

    }
}