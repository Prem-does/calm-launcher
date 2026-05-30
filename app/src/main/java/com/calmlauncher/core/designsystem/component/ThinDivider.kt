package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.calmlauncher.core.designsystem.theme.CalmDivider
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * A full-width hairline rule — white at 20% opacity, [Spacing.divider] (1dp) tall.
 * The single separator used between every list row, header and nav bar across the
 * launcher, matching the Stitch `border-b border-white/20` treatment.
 */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(Spacing.divider)
            .background(CalmDivider),
    )
}
