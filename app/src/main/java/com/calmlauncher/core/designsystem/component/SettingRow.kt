package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.calmlauncher.core.designsystem.theme.CalmError
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * A single configuration row. [title] is shown in [CalmType.bodyLg] (switched to
 * [CalmError] when [destructive], e.g. "Delete Account"). The trailing edge resolves,
 * in priority order, to: a custom [trailing] slot (typically a [CalmToggle]); a
 * [showChevron] arrow; or a [value] string in [CalmType.bodyMd] / [CalmGray].
 * Vertical padding is [Spacing.rowVertical] with a closing [ThinDivider]. Supplying
 * [onClick] makes the whole row tappable (no ripple).
 *
 * There is deliberately no subtitle slot. A settings list that explains each switch in
 * grey 12sp reads as a manual; a row that needs a sentence needs a better name.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
    showChevron: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val rowModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    val titleColor = if (destructive) CalmError else CalmWhite

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(rowModifier)
                .padding(
                    horizontal = Spacing.marginMobile,
                    vertical = Spacing.rowVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Text(
                text = title,
                style = CalmType.bodyLg,
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
            when {
                trailing != null -> trailing()
                showChevron -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CalmGray,
                )
                value != null -> Text(
                    text = value,
                    style = CalmType.bodyMd,
                    color = CalmGray,
                )
            }
        }
        ThinDivider()
    }
}

/**
 * An UPPERCASE section heading separating groups of [SettingRow]s (e.g. "ACCOUNT").
 * Rendered in [CalmType.labelLg] / [CalmGray] with [Spacing.stackLg] of top padding and
 * [Spacing.stackMd] below, matching the Stitch settings section labels.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = CalmType.labelLg,
        color = CalmGray,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.marginMobile,
                end = Spacing.marginMobile,
                top = Spacing.stackLg,
                bottom = Spacing.stackMd,
            ),
    )
}
