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
private const val MutedAlpha = 0.5f

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
            val remaining = ((1f - progress) * holdMillis).toLong().coerceAtLeast(0L)
            animate(
                initialValue = progress,
                targetValue = 1f,
                animationSpec = tween(durationMillis = remaining.toInt()),
            ) { value, _ -> progress = value }
            currentOnConfirm()
            progress = 0f
        } else if (progress > 0f) {
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
        Text(
            text = label.uppercase(),
            style = CalmType.labelLg,
            color = CalmWhite,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = Spacing.gutter),
        )
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