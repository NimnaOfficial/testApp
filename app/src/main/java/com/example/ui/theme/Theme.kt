package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AquaIntelColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = CyberGlassCard,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonBlue,
    onSecondary = Color.Black,
    tertiary = WarningOrange,
    error = CriticalRed,
    onError = Color.White,
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberCardBg,
    onSurface = TextPrimary,
    surfaceVariant = CyberGlassCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder
)

@Composable
fun AquaIntelTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AquaIntelColorScheme,
        typography = Typography,
        content = content
    )
}

