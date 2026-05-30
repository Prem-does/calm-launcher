package com.calmlauncher.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Root theme for the whole launcher. Always dark, always monochrome.
 */
@Composable
fun CalmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalmColorScheme,
        typography = CalmTypography,
        shapes = CalmShapes,
        content = content,
    )
}
