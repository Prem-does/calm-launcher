package com.calmlauncher.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import kotlinx.coroutines.delay

/** Shared full-bleed black backdrop for every launch-gate overlay. */
@Composable
private fun OverlayScrim(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val block = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalmBlack)
            // Absorb taps so the screen beneath the overlay can't be touched.
            .clickable(interactionSource = block, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/**
 * A breathing-pause countdown shown before a gated app finally opens. A large [seconds]
 * number (scaled-down [CalmType.heroTime]) sits under a "Pause." title with an
 * "Opening {appLabel}" caption and a [CalmButtonStyle.Text] Cancel. Counts down once per
 * second; on reaching zero it invokes [onFinished] exactly once. [onCancel] aborts.
 */
@Composable
fun CountdownOverlay(
    seconds: Int,
    appLabel: String,
    onFinished: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(seconds) {
        // Tick down to zero, one second at a time, then finish.
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        currentOnFinished()
    }

    OverlayScrim(modifier = modifier) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = Spacing.marginMobile),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Pause.", style = CalmType.headlineMd, color = CalmWhite)
            Text(
                text = remaining.coerceAtLeast(0).toString(),
                style = CalmType.heroTime,
                color = CalmWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = Spacing.stackMd),
            )
            Text(
                text = "Opening $appLabel",
                style = CalmType.bodyMd,
                color = CalmGray,
                textAlign = TextAlign.Center,
            )
            CalmButton(
                text = "Cancel",
                onClick = onCancel,
                style = CalmButtonStyle.Text,
                modifier = Modifier.padding(top = Spacing.stackLg),
            )
        }
    }
}

private val RingSize = 160.dp

/**
 * A guided breathing overlay. A single white ring gently scales and fades over ~4s per
 * breath while the caption alternates "Breathe in" / "Breathe out". After [cycles]
 * complete breaths [onComplete] is invoked once. A [CalmButtonStyle.Text] Cancel calls
 * [onCancel]. The gentle scale here is the one sanctioned exception to the no-motion
 * rule — it is purposeful, low-frequency and calming.
 */
@Composable
fun BreathOverlay(
    cycles: Int,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val halfCycleMs = 4000
    var breath by remember(cycles) { mutableIntStateOf(0) }
    var inhaling by remember(cycles) { mutableStateOf(true) }
    val currentOnComplete by rememberUpdatedState(onComplete)

    // One breath = an inhale half then an exhale half. Drive scale/alpha off [inhaling].
    val transition = updateTransition(targetState = inhaling, label = "breath")
    val ringScale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = halfCycleMs, easing = LinearEasing) },
        label = "ringScale",
    ) { inhale -> if (inhale) 1f else 0.6f }
    val ringAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = halfCycleMs, easing = LinearEasing) },
        label = "ringAlpha",
    ) { inhale -> if (inhale) 0.9f else 0.35f }

    LaunchedEffect(cycles) {
        while (breath < cycles) {
            inhaling = true
            delay(halfCycleMs.toLong())
            inhaling = false
            delay(halfCycleMs.toLong())
            breath += 1
        }
        currentOnComplete()
    }

    OverlayScrim(modifier = modifier) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = Spacing.marginMobile),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(RingSize)
                    .scale(ringScale)
                    .alpha(ringAlpha)
                    .drawBehind {
                        drawCircle(
                            color = CalmWhite,
                            radius = size.minDimension / 2f,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    },
            )
            Text(
                text = if (inhaling) "Breathe in" else "Breathe out",
                style = CalmType.headlineMd,
                color = CalmWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.stackLg),
            )
            CalmButton(
                text = "Cancel",
                onClick = onCancel,
                style = CalmButtonStyle.Text,
                modifier = Modifier.padding(top = Spacing.stackLg),
            )
        }
    }
}

/**
 * A minimal confirmation overlay. [message] is centered in [CalmType.bodyLg] above two
 * stacked-inline [CalmButton]s: the dismiss action ([dismissText]) is
 * [CalmButtonStyle.Outlined] and the confirm action ([confirmText]) is
 * [CalmButtonStyle.Filled].
 */
@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Open",
    dismissText: String = "Not now",
) {
    OverlayScrim(modifier = modifier) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .padding(horizontal = Spacing.marginMobile),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = CalmType.bodyLg,
                color = CalmWhite,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.padding(top = Spacing.stackLg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            ) {
                CalmButton(
                    text = dismissText,
                    onClick = onDismiss,
                    style = CalmButtonStyle.Outlined,
                )
                CalmButton(
                    text = confirmText,
                    onClick = onConfirm,
                    style = CalmButtonStyle.Filled,
                )
            }
        }
    }
}

/**
 * A reflective prompt asking the user why they want to open [appLabel] before a gated
 * launch. A heading sits above a single-line field styled with only a 1dp white bottom
 * border (CalmGray placeholder). Tapping a [suggestions] chip fills the field; a
 * [CalmButtonStyle.Filled] "Continue" forwards the current text to [onSubmit] and a
 * [CalmButtonStyle.Text] Cancel calls [onCancel]. Field text is held locally.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReasonPrompt(
    appLabel: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = listOf("Quick task", "Boredom", "Habit", "Need info"),
) {
    var text by remember { mutableStateOf("") }

    OverlayScrim(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = Spacing.marginMobile),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Why are you opening $appLabel?",
                style = CalmType.headlineMd,
                color = CalmWhite,
            )

            // Single-line field: bottom border only.
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = CalmType.bodyLg.copy(color = CalmWhite),
                cursorBrush = SolidColor(CalmWhite),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.stackLg)
                    .drawBehind {
                        val y = size.height
                        drawLine(
                            color = CalmWhite,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(vertical = Spacing.base),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(text = "Type a reason", style = CalmType.bodyLg, color = CalmGray)
                    }
                    inner()
                },
            )

            // Suggestion chips: small rectangular boxes that fill the field on tap.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.stackMd),
                horizontalArrangement = Arrangement.spacedBy(Spacing.base),
                verticalArrangement = Arrangement.spacedBy(Spacing.base),
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(label = suggestion, onClick = { text = suggestion })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.stackLg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalmButton(
                    text = "Continue",
                    onClick = { onSubmit(text) },
                    style = CalmButtonStyle.Filled,
                )
                CalmButton(
                    text = "Cancel",
                    onClick = onCancel,
                    style = CalmButtonStyle.Text,
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .border(width = 1.dp, color = CalmGray, shape = RoundedCornerShape(4.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Spacing.stackMd, vertical = Spacing.base),
    ) {
        Text(text = label, style = CalmType.labelMd, color = CalmWhite)
    }
}
