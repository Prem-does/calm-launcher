package com.calmlauncher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.fontSizeChoice
import com.calmlauncher.launcher.fontStyleChoice
import com.calmlauncher.launcher.uiDensityChoice

val CalmTypography = Typography(
	displayLarge = TextStyle(
		fontFamily = FontFamily.SansSerif,
		fontSize = 48.sp,
		lineHeight = 56.sp,
		letterSpacing = 0.sp
	),
	headlineLarge = TextStyle(
		fontFamily = FontFamily.SansSerif,
		fontSize = 32.sp,
		lineHeight = 40.sp,
		letterSpacing = 0.sp
	),
	headlineMedium = TextStyle(
		fontFamily = FontFamily.SansSerif,
		fontSize = 24.sp,
		lineHeight = 32.sp
	),
	bodyLarge = TextStyle(
		fontFamily = FontFamily.SansSerif,
		fontSize = 18.sp,
		lineHeight = 28.sp
	),
	bodyMedium = TextStyle(
		fontFamily = FontFamily.SansSerif,
		fontSize = 16.sp,
		lineHeight = 24.sp
	),
	labelSmall = TextStyle(
		fontFamily = FontFamily.Monospace,
		fontSize = 12.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.08.sp
	)
)

fun calmTypographyFor(settings: LauncherSettingsState): Typography {
	val baseScale = when (settings.fontSizeChoice()) {
		"Small" -> 0.92f
		"Large" -> 1.08f
		else -> 1.0f
	}
	val densityScale = when (settings.uiDensityChoice()) {
		"Compact" -> 0.94f
		"Comfortable" -> 1.05f
		else -> 1.0f
	}
	val scale = baseScale * densityScale
	val headlineFamily = when (settings.fontStyleChoice()) {
		"IBM Plex Sans" -> FontFamily.SansSerif
		"Helvetica Neue" -> FontFamily.SansSerif
		"Inter" -> FontFamily.SansSerif
		"Space Grotesk" -> FontFamily.SansSerif
		"Georgia" -> FontFamily.Serif
		else -> FontFamily.SansSerif
	}
	val bodyFamily = when (settings.fontStyleChoice()) {
		"IBM Plex Sans" -> FontFamily.SansSerif
		"Helvetica Neue" -> FontFamily.SansSerif
		"Inter" -> FontFamily.SansSerif
		"Space Grotesk" -> FontFamily.SansSerif
		"Georgia" -> FontFamily.Serif
		else -> FontFamily.SansSerif
	}

	return Typography(
		displayLarge = CalmTypography.displayLarge.copy(
			fontFamily = headlineFamily,
			fontSize = (48f * scale).sp,
			lineHeight = (56f * scale).sp
		),
		headlineLarge = CalmTypography.headlineLarge.copy(
			fontFamily = headlineFamily,
			fontSize = (32f * scale).sp,
			lineHeight = (40f * scale).sp
		),
		headlineMedium = CalmTypography.headlineMedium.copy(
			fontFamily = headlineFamily,
			fontSize = (24f * scale).sp,
			lineHeight = (32f * scale).sp
		),
		bodyLarge = CalmTypography.bodyLarge.copy(
			fontFamily = bodyFamily,
			fontSize = (18f * scale).sp,
			lineHeight = (28f * scale).sp
		),
		bodyMedium = CalmTypography.bodyMedium.copy(
			fontFamily = bodyFamily,
			fontSize = (16f * scale).sp,
			lineHeight = (24f * scale).sp
		),
		labelSmall = CalmTypography.labelSmall.copy(
			fontFamily = bodyFamily,
			fontSize = (12f * scale).sp,
			lineHeight = (16f * scale).sp
		)
	)
}
