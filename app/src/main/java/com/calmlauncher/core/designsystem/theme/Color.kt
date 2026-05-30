package com.calmlauncher.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val CalmDarkBackground = Color(0xFF000000)
internal val CalmDarkOnBackground = Color(0xFFFFFFFF)
internal val CalmDarkGray = Color(0xFFB3B3B3)
internal val CalmDarkGrayDim = Color(0xFF7E7576)
internal val CalmDarkSurfaceContainer = Color(0xFF1B1B1B)
internal val CalmDarkDivider = Color(0x33FFFFFF)
internal val CalmDarkError = Color(0xFFFF5252)

internal val CalmLightBackground = Color(0xFFF7F7F2)
internal val CalmLightOnBackground = Color(0xFF111111)
internal val CalmLightGray = Color(0xFF5E5E5E)
internal val CalmLightGrayDim = Color(0xFF9A9A9A)
internal val CalmLightSurfaceContainer = Color(0xFFE9E9E2)
internal val CalmLightDivider = Color(0x33000000)
internal val CalmLightError = Color(0xFFB3261E)

val CalmDarkColorScheme: ColorScheme = darkColorScheme(
    primary = CalmDarkOnBackground,
    onPrimary = CalmDarkBackground,
    primaryContainer = CalmDarkOnBackground,
    onPrimaryContainer = CalmDarkBackground,
    secondary = CalmDarkGray,
    onSecondary = CalmDarkBackground,
    background = CalmDarkBackground,
    onBackground = CalmDarkOnBackground,
    surface = CalmDarkBackground,
    onSurface = CalmDarkOnBackground,
    surfaceVariant = CalmDarkSurfaceContainer,
    onSurfaceVariant = CalmDarkGray,
    outline = CalmDarkGrayDim,
    outlineVariant = CalmDarkDivider,
    error = CalmDarkError,
    onError = CalmDarkBackground,
    scrim = CalmDarkBackground,
)

val CalmLightColorScheme: ColorScheme = lightColorScheme(
    primary = CalmLightOnBackground,
    onPrimary = CalmLightBackground,
    primaryContainer = CalmLightOnBackground,
    onPrimaryContainer = CalmLightBackground,
    secondary = CalmLightGray,
    onSecondary = CalmLightBackground,
    background = CalmLightBackground,
    onBackground = CalmLightOnBackground,
    surface = CalmLightBackground,
    onSurface = CalmLightOnBackground,
    surfaceVariant = CalmLightSurfaceContainer,
    onSurfaceVariant = CalmLightGray,
    outline = CalmLightGrayDim,
    outlineVariant = CalmLightDivider,
    error = CalmLightError,
    onError = CalmLightBackground,
    scrim = Color(0xFFEDEDE6),
)

object CalmPalette {
    var colorScheme: ColorScheme = CalmDarkColorScheme
}

val CalmBlack: Color
    get() = CalmPalette.colorScheme.background

val CalmGray: Color
    get() = CalmPalette.colorScheme.onSurfaceVariant

val CalmGrayDim: Color
    get() = CalmPalette.colorScheme.outline

val CalmSurfaceContainer: Color
    get() = CalmPalette.colorScheme.surfaceVariant

val CalmDivider: Color
    get() = CalmPalette.colorScheme.outlineVariant

val CalmError: Color
    get() = CalmPalette.colorScheme.error

val CalmWhite: Color
    get() = CalmPalette.colorScheme.onBackground
