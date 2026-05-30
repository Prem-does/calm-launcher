package com.calmlauncher.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.calmlauncher.R

/**
 * IBM Plex Sans, bundled as a single variable font. Weight axis is selected per
 * style via [FontVariation] (supported from API 26, our minSdk).
 */
private val plex = R.font.ibm_plex_sans

@OptIn(ExperimentalTextApi::class)
val IbmPlexSans: FontFamily = FontFamily(
    Font(
        plex,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        plex,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        plex,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

/**
 * Named type ramp matching the Stitch tokens 1:1. Use these directly for launcher
 * surfaces; [CalmTypography] mirrors them onto Material3 slots for stock components.
 */
object CalmType {
    /** The oversized home-screen clock. */
    val heroTime = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 80.sp,
        lineHeight = 84.sp,
        letterSpacing = (-0.05).em,
    )
    val headlineLg = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.02).em,
    )
    val headlineLgMobile = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em,
    )
    val headlineMd = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    )
    val bodyLg = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    )
    val bodyMd = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    /** Navigation + section labels — rendered UPPERCASE with wide tracking. */
    val labelLg = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.em,
    )
    val labelMd = TextStyle(
        fontFamily = IbmPlexSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
}

val CalmTypography: Typography = Typography(
    displayLarge = CalmType.heroTime,
    headlineLarge = CalmType.headlineLg,
    headlineMedium = CalmType.headlineMd,
    titleLarge = CalmType.headlineMd,
    titleMedium = CalmType.bodyLg,
    bodyLarge = CalmType.bodyLg,
    bodyMedium = CalmType.bodyMd,
    labelLarge = CalmType.labelLg,
    labelMedium = CalmType.labelMd,
    labelSmall = CalmType.labelMd,
)
