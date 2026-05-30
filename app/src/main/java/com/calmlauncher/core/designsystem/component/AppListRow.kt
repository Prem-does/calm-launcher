package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * A single tappable app entry in the launcher's text-mode app list. Full-width and
 * left-aligned, with [Spacing.rowVertical] vertical padding, [label] in
 * [CalmType.bodyLg] and a closing [ThinDivider].
 *
 * Signature interaction: while pressed the row inverts to the Stitch "active block" —
 * [CalmWhite] background with [CalmBlack] text — instead of showing a ripple, observed
 * via the [MutableInteractionSource]. [onLongClick] is wired through
 * [combinedClickable]; [leading] is an optional slot for an app icon (ICONS display
 * mode), shown before the label.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val background: Color = if (pressed) CalmWhite else CalmBlack
    val foreground: Color = if (pressed) CalmBlack else CalmWhite

    Column(modifier = modifier.fillMaxWidth().background(background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.rowVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            if (leading != null) {
                leading()
            }
            Text(text = label, style = CalmType.bodyLg, color = foreground)
        }
        ThinDivider()
    }
}
