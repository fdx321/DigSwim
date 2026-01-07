package com.digswim.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonGreen,
    tertiary = LightGray,
    background = Black,
    surface = DarkGray,
    onPrimary = Black,
    onSecondary = Black,
    onTertiary = White,
    onBackground = White,
    onSurface = White,
)

// We force Dark Theme as per PRD "Visual First, Dark Mode"
@Composable
fun DigSwimTheme(
    darkTheme: Boolean = true, // Force Dark
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to stick to our branding
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
