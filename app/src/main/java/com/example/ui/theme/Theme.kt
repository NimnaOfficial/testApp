package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AquaDarkScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CyberDarkBg,
    secondary = NeonGreen,
    onSecondary = CyberDarkBg,
    background = CyberDarkBg,
    surface = CyberCardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = CyberDarkBg
)

@Composable
fun AquaIntelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AquaDarkScheme,
        typography = AquaTypography,
        content = content
    )
}
