package com.whatsup.automation.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color.White,
    primaryContainer = DarkSurface,
    onPrimaryContainer = WhatsAppGreenLight,
    secondary = WhatsAppGreenDark,
    onSecondary = Color.White,
    background = DarkBgPrimary,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkBgSecondary,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkBgSecondary,
    surfaceContainerHighest = DarkBgSecondary,
    surfaceContainerLow = DarkBgPrimary,
    surfaceContainerLowest = DarkBgPrimary,
    error = RoseError,
    onError = Color.White
)

@Composable
fun WhatsUpTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = DarkBgPrimary.toArgb()
                window.navigationBarColor = DarkBgPrimary.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

