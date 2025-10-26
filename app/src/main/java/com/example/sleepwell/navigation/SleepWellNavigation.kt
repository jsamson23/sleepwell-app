package com.example.sleepwell.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sleepwell.ui.dashboard.DashboardScreen
import com.example.sleepwell.ui.settings.SettingsScreen
import com.example.sleepwell.ui.appselection.AppSelectionScreen
import com.example.sleepwell.ui.onboarding.OnboardingScreen
import com.example.sleepwell.utils.PermissionUtils

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
    object AppSelection : Screen("app_selection")
    object Onboarding : Screen("onboarding")
}

@Composable
fun SleepWellNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val startDestination = if (PermissionUtils.hasRequiredPermissions(context)) {
        Screen.Dashboard.route
    } else {
        Screen.Onboarding.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToAppSelection = {
                    navController.navigate(Screen.AppSelection.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AppSelection.route) {
            AppSelectionScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
    }
}