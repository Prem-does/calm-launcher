package com.calmlauncher.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.calmlauncher.domain.model.AppearanceSettings
import com.calmlauncher.domain.model.ThemeMode
import com.calmlauncher.domain.model.ThemePreference

/**
 * The current appearance, available anywhere under [CalmTheme].
 *
 * Exposed as a composition local so screens can read layout choices (grid columns, clock style,
 * density) without every one of them taking an appearance parameter or growing its own ViewModel.
 * Because it flows down through composition, changing any value recomposes the affected screens
 * immediately — which is what "applies without restarting the launcher" actually means here.
 */
val LocalAppearance = staticCompositionLocalOf { AppearanceSettings() }

/** Spacing scaled by the user's density choice. See [CalmSpacing]. */
val LocalSpacing = staticCompositionLocalOf { CalmSpacing() }

/**
 * Root theme for the whole launcher.
 *
 * Two entry points exist deliberately. This overload takes the full [AppearanceSettings] and is what
 * the app uses; the [ThemePreference] overload below is a narrow shim for surfaces rendered outside
 * the normal composition (the reminder overlay Activity) that only need light or dark.
 *
 * The launcher stays monochrome by default. An accent, when chosen, reaches only the Material
 * `primary`/`secondary`/`tertiary` roles — selection states, toggles, focus rings — while
 * `background`, `surface`, `onBackground` and `onSurface` stay black-and-white. That boundary is
 * what keeps personalisation from undoing the low-stimulation design the launcher exists for.
 */
@Composable
fun CalmTheme(
    appearance: AppearanceSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (appearance.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val baseScheme = if (dark) CalmDarkColorScheme else CalmLightColorScheme
    val colorScheme = remember(baseScheme, appearance.accent, dark) {
        if (appearance.accent.isMonochrome) {
            baseScheme
        } else {
            val accent = Color(
                if (dark) appearance.accent.darkArgb else appearance.accent.lightArgb,
            )
            // Only the accent roles move. Leaving background/surface/onSurface alone is what keeps
            // text and page backgrounds monochrome whichever colour is picked.
            baseScheme.copy(
                primary = accent,
                primaryContainer = accent,
                secondary = accent,
                tertiary = accent,
            )
        }
    }

    val typeScale = remember(appearance.fontStyle, appearance.fontScale) {
        CalmTypeScale(appearance.fontStyle, appearance.fontScale)
    }
    val typography = remember(typeScale) { typeScale.toMaterialTypography() }
    val spacing = remember(appearance.density) { CalmSpacing(appearance.density.multiplier) }

    // Mirrored onto the mutable holders for the handful of non-composable call sites that read
    // these as top-level properties (CalmBlack, CalmWhite, CalmType.*).
    CalmPalette.colorScheme = colorScheme
    CalmTypeTokens.scale = typeScale
    CalmSpacingTokens.scale = spacing

    CompositionLocalProvider(
        LocalAppearance provides appearance,
        LocalSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = CalmShapes,
            content = content,
        )
    }
}

/**
 * Light/dark-only entry point, for surfaces rendered outside the app's normal composition — the
 * full-screen reminder overlay, which runs in its own Activity and must be able to draw without
 * waiting for appearance to load.
 */
@Composable
fun CalmTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit,
) {
    CalmTheme(
        appearance = AppearanceSettings(
            themeMode = when (themePreference) {
                ThemePreference.LIGHT -> ThemeMode.LIGHT
                ThemePreference.DARK -> ThemeMode.DARK
            },
        ),
        content = content,
    )
}
