package com.ombi.mobile.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ombi.mobile.ui.screens.home.HomeScreen
import com.ombi.mobile.ui.screens.requests.RequestsScreen
import com.ombi.mobile.ui.screens.search.SearchScreen
import com.ombi.mobile.ui.screens.settings.SettingsScreen

/**
 * The main app shell shown after a successful login.
 *
 * Renders a [Scaffold] with a [NavigationBar] at the bottom and a [NavHost]
 * for the four primary destinations: Home, Search, Requests, and Settings.
 *
 * Navigation behaviour:
 * - Each tab saves and restores its scroll/back-stack state when switching tabs.
 * - Tapping the current tab pops back to its start destination (single-top).
 * - [onLogout] is threaded down to [SettingsScreen] so a sign-out can navigate
 *   back to the login screen, which is managed by the outer [OmbiNavGraph].
 */
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                // Pop up to the start destination so the back-stack
                                // doesn't grow unbounded when switching tabs
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)     { HomeScreen() }
            composable(Screen.Search.route)   { SearchScreen() }
            composable(Screen.Requests.route) { RequestsScreen() }
            composable(Screen.Settings.route) { SettingsScreen(onLogout = onLogout) }
        }
    }
}
