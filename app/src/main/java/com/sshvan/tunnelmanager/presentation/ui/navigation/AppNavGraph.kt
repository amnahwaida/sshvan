package com.sshvan.tunnelmanager.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sshvan.tunnelmanager.presentation.ui.home.HomeScreen
import com.sshvan.tunnelmanager.presentation.ui.profile.EditProfileScreen
import com.sshvan.tunnelmanager.presentation.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToAddProfile = {
                    navController.navigate(Screen.AddProfile.route)
                },
                onNavigateToEditProfile = { profileId ->
                    navController.navigate(Screen.EditProfile.createRoute(profileId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.AddProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditProfile.route,
            arguments = listOf(
                navArgument("profileId") { type = NavType.LongType }
            )
        ) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
