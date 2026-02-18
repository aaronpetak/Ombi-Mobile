package com.ombi.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth flow
    object ServerSetup : Screen("server_setup")
    object Login : Screen("login")

    // Main app (bottom nav)
    object Home : Screen("home")
    object Search : Screen("search")
    object Requests : Screen("requests")
    object Settings : Screen("settings")
}

/** Bottom nav items — order determines tab order. */
enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    Home(Screen.Home, "Home", Icons.Default.Home),
    Search(Screen.Search, "Search", Icons.Default.Search),
    Requests(Screen.Requests, "Requests", Icons.Default.List),
    Settings(Screen.Settings, "Settings", Icons.Default.Settings)
}
