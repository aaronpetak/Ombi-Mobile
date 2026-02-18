package com.ombi.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ombi.mobile.data.preferences.UserPreferences
import com.ombi.mobile.ui.screens.login.LoginScreen
import com.ombi.mobile.ui.screens.serversetup.ServerSetupScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Composable
fun OmbiNavGraph(userPreferences: UserPreferences = hiltViewModel<NavViewModel>().userPreferences) {
    val navController = rememberNavController()

    // Determine start destination: no server URL → setup, else login (auth check in LoginViewModel)
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
