package com.calmlauncher.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/** One line in an [AppActionSheet]. */
data class AppAction(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * The long-press menu behind every app in the launcher, wherever it appears — home
 * shortcuts, the drawer, search results. A full-bleed black surface with the app's name at
 * the top and a short stack of plain text actions beneath it; no card, no elevation, no
 * icons. Tapping outside or pressing back dismisses without acting.
 *
 * The point of having one component for this is that "unfavorite" is reachable from every
 * surface that can show an app, not just from Manage Apps.
 */
@Composable
fun AppActionSheet(
    appLabel: String,
    actions: List<AppAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true, onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scrim = remember { MutableInteractionSource() }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CalmBlack)
                .clickable(interactionSource = scrim, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(horizontal = Spacing.marginMobile),
            ) {
                Text(
                    text = appLabel,
                    style = CalmType.headlineMd,
                    color = CalmWhite,
                    maxLines = 2,
                )
                Text(
                    text = "Choose an action",
                    style = CalmType.labelMd,
                    color = CalmGray,
                    modifier = Modifier.padding(top = Spacing.stackSm, bottom = Spacing.stackMd),
                )
                ThinDivider()
                actions.forEach { action ->
                    ActionLine(label = action.label, onClick = action.onClick)
                }
                ActionLine(label = "Cancel", onClick = onDismiss, muted = true)
            }
        }
    }
}

@Composable
private fun ActionLine(
    label: String,
    onClick: () -> Unit,
    muted: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Column {
        Text(
            text = label,
            style = CalmType.bodyLg,
            color = if (muted) CalmGray else CalmWhite,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(vertical = Spacing.rowVertical),
        )
        ThinDivider()
    }
}
