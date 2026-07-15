package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.calmlauncher.core.designsystem.theme.CalmAppNameTextStyle
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * An oversized favorite shortcut for the home screen. [label] is rendered in
 * [CalmAppNameTextStyle] with [Spacing.base] vertical padding. Tap has no ripple,
 * matching the home list in the Stitch reference.
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
                style = CalmAppNameTextStyle,
                color = CalmWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
