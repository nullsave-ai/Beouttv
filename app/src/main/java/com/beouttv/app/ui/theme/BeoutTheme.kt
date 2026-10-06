package com.beouttv.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BeoutDarkColorScheme = darkColorScheme(
    primary = BeoutColors.PrimaryBlue,
    onPrimary = BeoutColors.TextPrimary,
    secondary = BeoutColors.BrightBlue,
    onSecondary = BeoutColors.TextPrimary,
    background = BeoutColors.Background,
    onBackground = BeoutColors.TextPrimary,
    surface = BeoutColors.Surface,
    onSurface = BeoutColors.TextPrimary,
    surfaceVariant = BeoutColors.Card,
    onSurfaceVariant = BeoutColors.TextSecondary,
    error = BeoutColors.Error,
    onError = BeoutColors.TextPrimary,
    outline = BeoutColors.Divider
)

/**
 * BEOUT TV is dark-first by design — the app always renders the dark
 * palette regardless of system theme, matching premium streaming apps.
 */
@Composable
fun BeoutTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BeoutDarkColorScheme,
        typography = BeoutTypography,
        shapes = BeoutShapes.Material3,
        content = content
    )
}
