package com.sshvan.tunnelmanager.presentation.ui.navigation

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddProfile : Screen("add_profile")
    data object EditProfile : Screen("edit_profile/{profileId}") {
        fun createRoute(profileId: Long) = "edit_profile/$profileId"
    }
    data object Settings : Screen("settings")
}
