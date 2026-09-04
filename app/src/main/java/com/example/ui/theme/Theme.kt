package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VelvetColorScheme = darkColorScheme(
    primary = VelvetBrightCrimson,
    onPrimary = Color.White,
    primaryContainer = VelvetDarkBurgundy,
    onPrimaryContainer = VelvetTextPrimary,
    secondary = VelvetDeepCrimson,
    onSecondary = Color.White,
    tertiary = VelvetMutedPurple,
    background = VelvetPureBlack,
    onBackground = VelvetTextPrimary,
    surface = VelvetSurface,
    onSurface = VelvetTextPrimary,
    surfaceVariant = VelvetSurfaceElevated,
    onSurfaceVariant = VelvetTextSecondary,
    outline = VelvetBorder
)

@Composable
fun VelvetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VelvetColorScheme,
        typography = Typography,
        content = content
    )
}
