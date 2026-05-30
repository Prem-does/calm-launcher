package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/** Opacity of the trailing chevron on a home favorite. */
private const val TrailingAlpha = 0.6f

/**
 * An oversized favorite shortcut for the home screen. [label] is rendered in
 * [CalmType.headlineLgMobile] with a trailing [Icons.AutoMirrored.Filled.ArrowForward]
 * at ~60% opacity, [Spacing.base] vertical padding and a closing [ThinDivider]. Tap
 * has no ripple, matching the home list in the Stitch reference.
 */
@Composable
fun HomeShortcutRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = CalmType.headlineLgMobile,
                color = CalmWhite,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = CalmWhite.copy(alpha = TrailingAlpha),
            )
        }
        ThinDivider()
    }
}
