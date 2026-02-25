package com.ombi.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ombi.mobile.data.preferences.UserPreferences
import com.ombi.mobile.ui.screens.login.LoginScreen
import com.ombi.mobile.ui.screens.serversetup.ServerSetupScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Root navigation graph for the entire application.
 *
 * Manages the auth flow — the sequence of screens the user passes through
 * before reaching the main app:
 *
 * 1. **ServerSetup** — shown on first launch (or if no server URL is saved)
 *    so the user can enter their Ombi instance URL.
 * 2. **Login** — username/password screen that obtains a JWT from Ombi.
 * 3. **Main** — the [MainScreen] with bottom navigation, shown after login.
 *
 * The start destination is determined synchronously using [runBlocking] once
 * at composition time — this is safe because it runs before the first frame
 * and the DataStore read completes quickly from disk cache.
 *
 * Each step pops itself off the back-stack so Back cannot return to a
 * previous auth screen.
 */
@Composable
fun OmbiNavGraph(userPreferences: UserPreferences = hiltViewModel<NavViewModel>().userPreferences) {
    val navController = rememberNavController()

    // No server URL saved → show setup; otherwise go straight to login
    // (whether the existing token is still valid is checked in LoginViewModel)
    val serverUrl = runBlocking { userPreferences.serverUrl.first() }
    val startDestination = if (serverUrl.isBlank()) Screen.ServerSetup.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.ServerSetup.route) {
            ServerSetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ServerSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onConfigureServer = {
                    navController.navigate(Screen.ServerSetup.route)
                }
            )
        }

        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
