package com.calmlauncher.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Strictly achromatic palette extracted from the Stitch "Monochrome Swiss Launcher"
 * design system. Pure black surfaces, pure white primary, mid-gray for secondary
 * information, and a single error red reserved for destructive actions.
 */
val CalmBlack = Color(0xFF000000)
val CalmWhite = Color(0xFFFFFFFF)

/** Secondary / supporting text (on-surface-variant). */
val CalmGray = Color(0xFFB3B3B3)

/** Disabled / lowest-emphasis text and outlines. */
val CalmGrayDim = Color(0xFF7E7576)

/** Subtle container fill for cards (used sparingly). */
val CalmSurfaceContainer = Color(0xFF1B1B1B)

/** 0.5pt dividers — white at 20% opacity. */
val CalmDivider = Color(0x33FFFFFF)

/** Destructive accent only. */
val CalmError = Color(0xFFFF5252)

val CalmColorScheme: ColorScheme = darkColorScheme(
    primary = CalmWhite,
    onPrimary = CalmBlack,
    primaryContainer = CalmWhite,
    onPrimaryContainer = CalmBlack,
    secondary = CalmGray,
    onSecondary = CalmBlack,
    background = CalmBlack,
    onBackground = CalmWhite,
    surface = CalmBlack,
    onSurface = CalmWhite,
    surfaceVariant = CalmSurfaceContainer,
    onSurfaceVariant = CalmGray,
    outline = CalmGrayDim,
    outlineVariant = CalmDivider,
    error = CalmError,
    onError = CalmBlack,
    scrim = CalmBlack,
)
