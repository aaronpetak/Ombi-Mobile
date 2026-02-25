package com.ombi.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Root Material 3 theme for the Ombi app.
 *
 * Colour-scheme priority:
 * 1. **Dynamic colour** (Android 12+ / Material You) — wallpaper-derived palette
 *    when [dynamicColor] is true and the device runs API 31+.
 * 2. **Static dark scheme** ([DarkColorScheme]) when [darkTheme] is true.
 * 3. **Static light scheme** ([LightColorScheme]) as the fallback.
 *
 * [darkTheme] is driven by the user's in-app theme preference resolved in
 * [com.ombi.mobile.MainActivity] from [com.ombi.mobile.data.preferences.UserPreferences].
 */
@Composable
fun OmbiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color uses the wallpaper on Android 12+ (Material You)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
