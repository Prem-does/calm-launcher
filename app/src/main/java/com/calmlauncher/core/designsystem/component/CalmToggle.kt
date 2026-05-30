package com.calmlauncher.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmMotion
import com.calmlauncher.core.designsystem.theme.CalmWhite

private val TrackWidth = 40.dp
private val TrackHeight = 20.dp
private val KnobSize = 12.dp

/** Inset of the knob from the track edge so it reads centered: (20 - 12) / 2. */
private val KnobInset = 4.dp

/** Opacity applied to the whole control while disabled. */
private const val DisabledAlpha = 0.4f

/**
 * The launcher's switch: a 40dp x 20dp pill outlined with a 1dp [CalmWhite] border and
 * a 12dp solid white knob. The knob rests at the left edge when [checked] is false and
 * slides to the right edge when true, animated over [CalmMotion.FADE_MS] (200ms) — the
 * only motion permitted on the control. Matches the Stitch greyscale-mode toggle.
 * Toggling is suppressed and dimmed to 40% when [enabled] is false.
 */
@Composable
fun CalmToggle(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val knobOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - KnobSize - KnobInset else KnobInset,
        animationSpec = tween(durationMillis = CalmMotion.FADE_MS),
        label = "knobOffset",
    )
    val contentAlpha = if (enabled) 1f else DisabledAlpha

    Box(
        modifier = modifier
            .size(width = TrackWidth, height = TrackHeight)
            .clip(CircleShape)
            .border(width = 1.dp, color = CalmWhite.copy(alpha = contentAlpha), shape = CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(KnobSize)
                .clip(CircleShape)
                .background(CalmWhite.copy(alpha = contentAlpha)),
        )
    }
}
