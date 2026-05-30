package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite

/** Visual emphasis levels for [CalmButton]. */
enum class CalmButtonStyle { Outlined, Filled, Text }

/** 4dp rounding from the Stitch shape scale (`borderRadius.DEFAULT`). */
private val ButtonShape = RoundedCornerShape(4.dp)

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/** Opacity applied to the whole button while disabled. */
private const val DisabledAlpha = 0.4f

/**
 * The launcher's text button, in three monochrome emphasis levels:
 *  - [CalmButtonStyle.Outlined] — transparent fill, 1dp [CalmWhite] border, white text.
 *  - [CalmButtonStyle.Filled] — solid [CalmWhite] fill with [CalmBlack] text.
 *  - [CalmButtonStyle.Text] — no fill or border, plain white text.
 *
 * Label is [CalmType.labelLg] with slight (4dp) rounding. Ripple is suppressed; when
 * [enabled] is false the control is non-interactive and dimmed to 40%.
 */
@Composable
fun CalmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CalmButtonStyle = CalmButtonStyle.Outlined,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val alpha = if (enabled) 1f else DisabledAlpha

    val containerColor: Color = when (style) {
        CalmButtonStyle.Filled -> CalmWhite.copy(alpha = alpha)
        else -> Color.Transparent
    }
    val contentColor: Color = when (style) {
        CalmButtonStyle.Filled -> CalmBlack
        else -> CalmWhite
    }

    var box = modifier
        .clip(ButtonShape)
        .background(containerColor)

    if (style == CalmButtonStyle.Outlined) {
        box = box.border(width = 1.dp, color = CalmWhite.copy(alpha = alpha), shape = ButtonShape)
    }

    box = box
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
        .padding(ButtonPadding)

    Box(modifier = box, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = CalmType.labelLg,
            color = contentColor.copy(alpha = alpha),
        )
    }
}
