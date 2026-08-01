package com.calmlauncher.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The 8px base spacing scale, scaled by the user's density choice.
 *
 * Whitespace only. Density changes how far apart things sit and never what is shown — the same apps
 * appear in a compact layout as in a spacious one, just closer together. [bottomNavHeight] and
 * [divider] are deliberately left unscaled: a nav bar that shrinks below the 48dp minimum touch
 * target, or a hairline divider that rounds away to nothing, would be an accessibility regression
 * dressed up as a preference.
 */
class CalmSpacing(private val multiplier: Float = 1f) {
    private fun scale(base: Float): Dp = (base * multiplier).dp

    val stackSm: Dp = scale(4f)
    val base: Dp = scale(8f)
    val stackMd: Dp = scale(12f)
    val gutter: Dp = scale(16f)
    val marginMobile: Dp = scale(24f)
    val stackLg: Dp = scale(32f)

    /** Standard list-row vertical padding (Stitch `py-stack-md`). */
    val rowVertical: Dp = scale(12f)

    /** Height of the bottom navigation bar. Unscaled — this is a touch target. */
    val bottomNavHeight: Dp = 64.dp

    /** Divider thickness (0.5pt feel). Unscaled — already at the minimum visible width. */
    val divider: Dp = 1.dp
}

/**
 * Holder for the active spacing scale.
 *
 * A mutable holder rather than only a composition local because [Spacing] is read from top-level
 * `val`s throughout the codebase, outside any composable. Composables that can reach
 * [LocalSpacing] should prefer it; everything else reads these tokens, which [CalmTheme] keeps in
 * step with the current density. Same pattern as [CalmPalette] and [CalmTypeTokens].
 */
object CalmSpacingTokens {
    var scale: CalmSpacing = CalmSpacing()
}

/**
 * Named spacing tokens, delegating to the active [CalmSpacing]. Existing `Spacing.gutter` call sites
 * pick up a density change with no edits.
 */
object Spacing {
    val stackSm: Dp get() = CalmSpacingTokens.scale.stackSm
    val base: Dp get() = CalmSpacingTokens.scale.base
    val stackMd: Dp get() = CalmSpacingTokens.scale.stackMd
    val gutter: Dp get() = CalmSpacingTokens.scale.gutter
    val marginMobile: Dp get() = CalmSpacingTokens.scale.marginMobile
    val stackLg: Dp get() = CalmSpacingTokens.scale.stackLg
    val rowVertical: Dp get() = CalmSpacingTokens.scale.rowVertical
    val bottomNavHeight: Dp get() = CalmSpacingTokens.scale.bottomNavHeight
    val divider: Dp get() = CalmSpacingTokens.scale.divider
}
