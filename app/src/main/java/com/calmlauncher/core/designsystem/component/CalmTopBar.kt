package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/** Minimum touch-target height for the bar content row. */
private val BarMinHeight = 56.dp

/**
 * The launcher status bar: a faux signal indicator on the left, the [centerLabel]
 * (default "SHARP") centered, and [batteryText] on the right. Mirrors the Stitch
 * TopAppBar — horizontal [Spacing.marginMobile] gutters, status-bar inset padding and
 * a closing [ThinDivider]. [signalText] is shown beside the signal glyph when non-blank
 * (e.g. a carrier or "5G" tag); it is omitted otherwise.
 */
@Composable
fun CalmStatusBar(
    batteryText: String,
    signalText: String,
    modifier: Modifier = Modifier,
    centerLabel: String = "SHARP",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.base)
                .heightIn(min = BarMinHeight),
            contentAlignment = Alignment.Center,
        ) {
            // Leading: signal indicator (+ optional carrier text).
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                Icon(
                    imageVector = signalIcon(signalText),
                    contentDescription = null,
                    tint = CalmWhite,
                )
                if (signalText.isNotBlank()) {
                    Text(text = signalText, style = CalmType.labelMd, color = CalmWhite)
                }
            }

            // Center: brand / context label.
            Text(
                text = centerLabel,
                style = CalmType.headlineMd,
                color = CalmWhite,
                modifier = Modifier.align(Alignment.Center),
            )

            // Trailing: battery.
            Text(
                text = batteryText,
                style = CalmType.labelMd,
                color = CalmWhite,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        ThinDivider()
    }
}

private fun signalIcon(signalText: String) = when (signalText) {
    "Airplane" -> Icons.Filled.AirplanemodeActive
    "Offline" -> Icons.Filled.SignalCellularOff
    else -> Icons.Filled.SignalCellularAlt
}

/**
 * A back header for focused sub-journeys (Settings et al.): an
 * [Icons.AutoMirrored.Filled.ArrowBack] arrow followed by [title] in
 * [CalmType.headlineMd], left-aligned, with a closing [ThinDivider]. The whole leading
 * cluster is the tap target for [onBack]; indication is suppressed for the e-ink feel.
 */
@Composable
fun CalmBackBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = BarMinHeight)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onBack,
                )
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = CalmWhite,
            )
            Text(text = title, style = CalmType.headlineMd, color = CalmWhite)
        }
        ThinDivider()
    }
}
