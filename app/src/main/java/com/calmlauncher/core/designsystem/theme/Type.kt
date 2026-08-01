package com.calmlauncher.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.calmlauncher.domain.model.FontScale
import com.calmlauncher.domain.model.FontStyle

/**
 * Global launcher font family. The platform sans-serif stack is the default typography base, so
 * every screen picks up the same Helvetica Neue-style sans treatment.
 */
val HelveticaNeue: FontFamily = FontFamily.SansSerif

/**
 * The type ramp, resolved for a chosen family and size multiplier.
 *
 * Sizes are multiplied rather than substituted so the ramp keeps its proportions: the hero clock
 * stays dramatically larger than body text at every scale, which is much of the character of the
 * home screen. The multiplier composes with the device's own font-size setting rather than replacing
 * it — `sp` units are still scaled by the platform, so someone who has enlarged text system-wide
 * keeps that, and this applies on top.
 */
class CalmTypeScale(
    fontStyle: FontStyle = FontStyle.SANS,
    fontScale: FontScale = FontScale.NORMAL,
) {
    private val family: FontFamily = when (fontStyle) {
        // Plex is a bundled resource font, and resolving it needs a Context that tokens don't have.
        // The platform sans stands in at this level; it is visually the closest of the four.
        FontStyle.PLEX -> FontFamily.SansSerif
        FontStyle.SANS -> FontFamily.SansSerif
        FontStyle.SERIF -> FontFamily.Serif
        FontStyle.MONO -> FontFamily.Monospace
    }

    private val multiplier: Float = fontScale.multiplier

    private fun sp(value: Float): TextUnit = (value * multiplier).sp

    /** The oversized home-screen clock. */
    val heroTime = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = sp(80f),
        lineHeight = sp(84f),
        letterSpacing = (-0.05).em,
    )
    val headlineLg = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = sp(48f),
        lineHeight = sp(56f),
        letterSpacing = (-0.02).em,
    )
    val headlineLgMobile = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = sp(32f),
        lineHeight = sp(40f),
        letterSpacing = (-0.02).em,
    )
    val headlineMd = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = sp(24f),
        lineHeight = sp(32f),
    )
    val bodyLg = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = sp(18f),
        lineHeight = sp(28f),
    )
    val bodyMd = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = sp(16f),
        lineHeight = sp(24f),
    )

    /** Navigation + section labels — rendered UPPERCASE with wide tracking. */
    val labelLg = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = sp(14f),
        lineHeight = sp(20f),
        letterSpacing = 0.05.em,
    )
    val labelMd = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = sp(12f),
        lineHeight = sp(16f),
    )

    /** Mirror the ramp onto Material3 slots, for stock components. */
    fun toMaterialTypography(): Typography = Typography(
        displayLarge = heroTime,
        headlineLarge = headlineLg,
        headlineMedium = headlineMd,
        titleLarge = headlineMd,
        titleMedium = bodyLg,
        bodyLarge = bodyLg,
        bodyMedium = bodyMd,
        labelLarge = labelLg,
        labelMedium = labelMd,
        labelSmall = labelMd,
    )
}

/**
 * Holder for the active type scale.
 *
 * A mutable holder rather than a composition local because [CalmType] is read from top-level `val`s
 * throughout the codebase, outside any composable. [CalmTheme] updates it whenever appearance
 * changes, so the tokens below always reflect the current settings. Same pattern — and the same
 * trade-off — as [CalmPalette].
 */
object CalmTypeTokens {
    var scale: CalmTypeScale = CalmTypeScale()
}

/**
 * Named type ramp. These delegate to the active [CalmTypeScale], so a font-style or font-size change
 * in Customization is picked up by every existing `CalmType.bodyMd` call site with no edits to it.
 */
object CalmType {
    val heroTime: TextStyle get() = CalmTypeTokens.scale.heroTime
    val headlineLg: TextStyle get() = CalmTypeTokens.scale.headlineLg
    val headlineLgMobile: TextStyle get() = CalmTypeTokens.scale.headlineLgMobile
    val headlineMd: TextStyle get() = CalmTypeTokens.scale.headlineMd
    val bodyLg: TextStyle get() = CalmTypeTokens.scale.bodyLg
    val bodyMd: TextStyle get() = CalmTypeTokens.scale.bodyMd
    val labelLg: TextStyle get() = CalmTypeTokens.scale.labelLg
    val labelMd: TextStyle get() = CalmTypeTokens.scale.labelMd
}

/** Default Material typography, for previews and surfaces built before the theme is applied. */
val CalmTypography: Typography = CalmTypeScale().toMaterialTypography()
