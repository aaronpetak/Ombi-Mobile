package com.ombi.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ombi.mobile.data.preferences.UserPreferences
import com.ombi.mobile.ui.screens.login.LoginScreen
import com.ombi.mobile.ui.screens.serversetup.ServerSetupScreen

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
 * The start destination is [Screen.Loading], a transient spinner. The saved
 * server URL is read asynchronously from DataStore (never blocking the main
 * thread); on the first emission we navigate to ServerSetup (no URL saved) or
 * Login. A [rememberSaveable] guard ensures this initial routing happens only
 * once, so a later server-URL change (e.g. from Settings) cannot bounce the
 * user back to Login.
 *
 * Each step pops itself off the back-stack so Back cannot return to a
 * previous auth screen.
 */
@Composable
fun OmbiNavGraph(userPreferences: UserPreferences = hiltViewModel<NavViewModel>().userPreferences) {
    val navController = rememberNavController()

    // Read the saved server URL reactively; null = not yet loaded.
    val serverUrl by userPreferences.serverUrl.collectAsStateWithLifecycle(initialValue = null)

    // Route off the Loading screen exactly once, on the first non-null emission.
    // No server URL saved → setup; otherwise login (token validity is checked
    // in LoginViewModel).
    var hasNavigated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(serverUrl) {
        val url = serverUrl
        if (url != null && !hasNavigated) {
            hasNavigated = true
            val target = if (url.isBlank()) Screen.ServerSetup.route else Screen.Login.route
            navController.navigate(target) {
                popUpTo(Screen.Loading.route) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Loading.route) {
        composable(Screen.Loading.route) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

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
