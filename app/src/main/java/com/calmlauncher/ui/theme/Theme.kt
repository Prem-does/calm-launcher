package com.calmlauncher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.animationLevelChoice
import com.calmlauncher.launcher.blackAndWhiteOnly
import com.calmlauncher.launcher.grayscaleModeChoice
import com.calmlauncher.launcher.slowModeEnabled
import com.calmlauncher.launcher.themeChoice
import com.calmlauncher.launcher.wallpaperChoice

private val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    background = Black,
    onBackground = White,
    surface = Gray900,
    onSurface = White
)

val LocalLauncherSettings = staticCompositionLocalOf { LauncherSettingsState() }

private fun colorSchemeFor(settings: LauncherSettingsState) = when (settings.themeChoice()) {
    "Paper White" -> darkColorScheme(
        primary = Black,
        onPrimary = White,
        background = when (settings.wallpaperChoice()) {
            "Solid White" -> Color(0xFFF5F2EA)
            "Custom Minimal" -> Color(0xFFF0EEE6)
            else -> Color(0xFFF2F0E8)
        },
        onBackground = Black,
        surface = Color(0xFFE3E0D8),
        onSurface = Black
    )
    "Dark Gray" -> darkColorScheme(
        primary = White,
        onPrimary = Black,
        background = when (settings.wallpaperChoice()) {
            "Solid Black" -> Color(0xFF151515)
            "Custom Minimal" -> Color(0xFF1B1B1B)
            else -> Color(0xFF202020)
        },
        onBackground = White,
        surface = Color(0xFF2A2A2A),
        onSurface = White
    )
    else -> darkColorScheme(
        primary = White,
        onPrimary = Black,
        background = when (settings.wallpaperChoice()) {
            "Solid White" -> Color(0xFFFBFBF8)
            "Custom Minimal" -> Color(0xFF191919)
            else -> Color(0xFF131313)
        },
        onBackground = White,
        surface = Color(0xFF232323),
        onSurface = White
    )
}

@Composable
fun CalmLauncherTheme(settings: LauncherSettingsState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLauncherSettings provides settings) {
        MaterialTheme(
            colorScheme = colorSchemeFor(settings),
            typography = calmTypographyFor(settings),
            shapes = CalmShapes,
            content = content
        )
    }
}

fun launcherAnimationEnabled(settings: LauncherSettingsState): Boolean {
    return when (settings.animationLevelChoice()) {
        "None" -> false
        "Slow Fade" -> true
        else -> true
    }
}

fun launcherMotionDurationMillis(settings: LauncherSettingsState): Int {
    if (settings.slowModeEnabled()) {
        return when (settings.animationLevelChoice()) {
            "None" -> 0
            else -> 520
        }
    }
    return when (settings.animationLevelChoice()) {
        "None" -> 0
        "Soft Fade" -> 180
        "Slow Fade" -> 320
        else -> 180
    }
}
