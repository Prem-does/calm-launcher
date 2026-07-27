package com.calmlauncher.feature.gate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import kotlinx.coroutines.delay

/** How long the user sits with a plain block before the way out appears. */
const val BlockCountdownSeconds = 5

/** How long the user sits with an exceeded app limit before an extension is offered. */
const val LimitCountdownSeconds = 10

/**
 * The screen a blocked launch lands on: a black canvas, the app's name, why it stopped, and
 * a large number counting down. Nothing is tappable while the number runs — the whole point
 * is that the pause is the feature, not an obstacle to click past.
 *
 * When the count reaches zero the actions appear. [onOverride] is the costly one (extending
 * a limit) and is deliberately the thing the countdown gates; [onDismiss] simply walks away
 * and is always the second option.
 *
 * @param seconds how long to hold before revealing actions.
 * @param title the headline, e.g. "Limit reached".
 * @param appLabel the app the user tried to open.
 * @param detail one line of context under the app name — usage against the limit, or the
 *   reason the mode engine refused the launch.
 * @param overrideLabel label for the escape hatch, or null when none is available.
 */
@Composable
fun BlockCountdownOverlay(
    seconds: Int,
    title: String,
    appLabel: String,
    detail: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    overrideLabel: String? = null,
    onOverride: (() -> Unit)? = null,
    footnote: String? = null,
) {
    var remaining by remember(appLabel, seconds) { mutableIntStateOf(seconds) }
    // Guards the override against a double-tap while the coordinator is still working.
    var overrideRequested by remember(appLabel) { mutableStateOf(false) }

    LaunchedEffect(appLabel, seconds) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }

    // Back must not be an escape hatch from a block; the countdown is the only way through.
    BackHandler(enabled = true) {}

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CalmBlack)
                // Swallow every touch so the screen underneath can't be reached.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
                .onKeyEvent { true }
                .semantics { isTraversalGroup = true },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .focusGroup()
                    .systemBarsPadding()
                    .padding(horizontal = Spacing.marginMobile)
                    .widthIn(max = 360.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = CalmType.headlineMd,
                    color = CalmWhite,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = appLabel,
                    style = CalmType.bodyLg,
                    color = CalmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.stackSm),
                )

                Text(
                    text = remaining.coerceAtLeast(0).toString(),
                    style = CalmType.heroTime,
                    color = CalmWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.stackLg),
                )

                Text(
                    text = detail,
                    style = CalmType.bodyMd,
                    color = CalmGray,
                    textAlign = TextAlign.Center,
                )

                // Actions only exist once the pause is over. Until then the screen is a
                // dead end on purpose.
                if (remaining <= 0) {
                    if (overrideLabel != null && onOverride != null) {
                        CalmButton(
                            text = overrideLabel,
                            style = CalmButtonStyle.Outlined,
                            onClick = {
                                if (!overrideRequested) {
                                    overrideRequested = true
                                    onOverride()
                                }
                            },
                            modifier = Modifier.padding(top = Spacing.stackLg),
                        )
                    }
                    CalmButton(
                        text = "Back to home",
                        style = CalmButtonStyle.Filled,
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = Spacing.stackMd),
                    )
                    if (footnote != null) {
                        Text(
                            text = footnote,
                            style = CalmType.labelMd,
                            color = CalmGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = Spacing.stackMd),
                        )
                    }
                }
            }
        }
    }
}
