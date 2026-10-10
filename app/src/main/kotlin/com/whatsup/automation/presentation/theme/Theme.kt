package com.whatsup.automation.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = WhatsAppGreenDark,
    secondary = WhatsAppGreenDark,
    onSecondary = Color.White,
    background = DarkBgPrimary,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkBgSecondary,
    onSurfaceVariant = TextSecondary,
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
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

