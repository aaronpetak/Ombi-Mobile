package com.ombi.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defines every navigation destination in the app.
 *
 * Navigation is split into two levels:
 * - **Auth flow** ([ServerSetup], [Login]) — managed by the outer [OmbiNavGraph].
 * - **Main app** ([Home], [Search], [Requests], [Settings]) — managed by the
 *   inner [MainScreen] bottom navigation host.
 */
sealed class Screen(val route: String) {
    // Transient start destination shown while the saved server URL is read
    // asynchronously; navigation moves off it on the first emission.
    object Loading : Screen("loading")

    // Auth flow
    object ServerSetup : Screen("server_setup")
    object Login       : Screen("login")

    // Main app (bottom nav)
    object Home      : Screen("home")
    object Search    : Screen("search")
    object Requests  : Screen("requests")
    object Settings  : Screen("settings")
}

/**
 * Maps each bottom-nav tab to its [Screen], display label, and icon.
 * The declaration order determines the left-to-right tab order in the UI.
 */
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
