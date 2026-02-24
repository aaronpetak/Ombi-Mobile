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
import com.ombi.mobile.ui.navigation.OmbiNavGraph
import com.ombi.mobile.ui.theme.OmbiTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by userPreferences.theme.collectAsState(initial = "dark")
            val darkTheme = when (theme) {
                "dark"  -> true
                "light" -> false
                else    -> isSystemInDarkTheme()
            }
            OmbiTheme(darkTheme = darkTheme) {
                OmbiNavGraph()
            }
        }
    }
}
