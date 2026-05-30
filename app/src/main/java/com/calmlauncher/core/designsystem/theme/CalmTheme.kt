package com.calmlauncher.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.calmlauncher.domain.model.ThemePreference

/**
 * Root theme for the whole launcher. Stays monochrome while switching between light and
 * dark appearances.
 */
@Composable
fun CalmTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themePreference) {
        ThemePreference.LIGHT -> CalmLightColorScheme
        ThemePreference.DARK -> CalmDarkColorScheme
    }

    CalmPalette.colorScheme = colorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CalmTypography,
        shapes = CalmShapes,
        content = content,
    )
}
