package com.calmlauncher.core.designsystem.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The oversized style used for app names on the home screen and in the app list.
 *
 * Deliberately a `get()` rather than an initialised `val`. A top-level `val` is computed once, at
 * first access, and would therefore freeze whichever font and size happened to be active then —
 * leaving every app name immune to a later Customization change while the rest of the launcher
 * updated around it. Reading through on each access keeps it in step.
 *
 * The size is derived from the active ramp rather than hard-coded, so the user's text-size choice
 * scales app names too. 24sp × 1.5 reproduces the original 36sp at the default scale.
 */
val CalmAppNameTextStyle: TextStyle
    get() {
        val base = CalmType.headlineMd
        val size = base.fontSize * APP_NAME_RATIO
        return base.copy(
            fontWeight = FontWeight.Medium,
            fontSize = size,
            lineHeight = size * LINE_HEIGHT_RATIO,
            letterSpacing = 0.sp,
        )
    }

/** 24sp × 1.5 = the original 36sp app-name size at the default text scale. */
private const val APP_NAME_RATIO = 1.5f

/** 46 / 36, preserving the original line-height proportion. */
private const val LINE_HEIGHT_RATIO = 1.28f
