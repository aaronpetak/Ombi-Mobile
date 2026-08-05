package com.ombi.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import com.ombi.mobile.data.preferences.UserPreferences
import com.ombi.mobile.data.repository.AuthRepository
import com.ombi.mobile.ui.navigation.OmbiNavGraph
import com.ombi.mobile.ui.theme.OmbiTheme
import javax.inject.Inject

/**
 * The single activity that hosts the entire Compose UI.
 *
 * Responsibilities:
 * - Enables edge-to-edge rendering so content draws behind system bars.
 * - Reads the user's theme preference (dark / light / system) from
 *   [UserPreferences] and applies [OmbiTheme] accordingly.
 * - Renders [OmbiNavGraph] which manages the full navigation stack.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Observe the persisted theme preference; default to dark until the
            // DataStore emits its first value to avoid a flash of light mode.
            val theme by userPreferences.theme.collectAsState(initial = "dark")
            val darkTheme = when (theme) {
                "dark"  -> true
                "light" -> false
                else    -> isSystemInDarkTheme() // "system" — follow the OS setting
            }
            OmbiTheme(darkTheme = darkTheme) {
                OmbiNavGraph(
                    userPreferences = userPreferences,
                    authRepository = authRepository
                )
            }
        }
    }
}
