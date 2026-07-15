package com.calmlauncher.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

private val BarWidth = 192.dp
private val BarHeight = 2.dp

/** Resting / sub-label opacity for the hold control. */
private const val MutedAlpha = 0.5f

/**
 * A deliberate "hold to confirm" control, used as the Focus-Mode exit. A thin 2dp track
 * fills left-to-right while the user presses and holds; reaching 100% over [holdMillis]
 * invokes [onConfirm]. Releasing early animates the fill back to 0 so the gesture must
 * be completed in one continuous press. Below the bar, [label] is shown UPPERCASE in
 * [CalmType.labelLg] and the optional [subLabel] (default "Hold to exit") in
 * [CalmType.labelMd] at ~50% opacity.
 *
 * Implementation: the gesture only flips a [pressed] flag (releasing on up *and* on
 * cancel); a [LaunchedEffect] keyed on that flag owns the animation, so letting go
 * reliably cancels the fill and rewinds — a tap can never accidentally confirm.
 */
@Composable
fun HoldToConfirm(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 1500,
    subLabel: String? = "Hold to exit",
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(pressed) {
        if (pressed) {
            // Fill from the current value to full over the remaining time slice.
            val remaining = ((1f - progress) * holdMillis).toLong().coerceAtLeast(0L)
            animate(
                initialValue = progress,
                targetValue = 1f,
                animationSpec = tween(durationMillis = remaining.toInt()),
            ) { value, _ -> progress = value }
            // Reaching here without cancellation means the hold completed.
            currentOnConfirm()
            progress = 0f
        } else if (progress > 0f) {
            // Released early — rewind to empty proportionally to how far we got.
            val rewind = (progress * holdMillis).toLong().coerceAtLeast(0L)
            animate(
                initialValue = progress,
                targetValue = 0f,
                animationSpec = tween(durationMillis = rewind.toInt()),
            ) { value, _ -> progress = value }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(BarWidth)
                .height(BarHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(CalmWhite.copy(alpha = MutedAlpha)),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(BarHeight)
                    .background(CalmWhite),
            )
        }

        Text(
            text = label.uppercase(),
            style = CalmType.labelLg,
            color = CalmWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.gutter),
        )
        if (subLabel != null) {
            Text(
                text = subLabel,
                style = CalmType.labelMd,
                color = CalmWhite.copy(alpha = MutedAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.stackSm),
            )
        }
    }
}
