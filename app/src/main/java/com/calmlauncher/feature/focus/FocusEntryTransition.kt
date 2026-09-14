package com.calmlauncher.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite

@Composable
fun FocusEntryTransition(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {}

    if (entered) {
        FocusScreen(onExit = onExit, modifier = modifier)
    } else {
        val alpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = 700
                0f at 0
                0.5f at 350
                1f at 700
            },
            finishedListener = { entered = true },
            label = "focus entry opacity",
        )
        val blurRadius by animateDpAsState(
            targetValue = 0.dp,
            animationSpec = keyframes {
                durationMillis = 700
                10.dp at 0
                5.dp at 350
                0.dp at 700
            },
            label = "focus entry blur",
        )
        val verticalOffset by animateDpAsState(
            targetValue = 0.dp,
            animationSpec = keyframes {
                durationMillis = 700
                (-50).dp at 0
                5.dp at 350
                0.dp at 700
            },
            label = "focus entry offset",
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CalmBlack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "focus",
                style = CalmType.heroTime,
                color = CalmWhite,
                modifier = Modifier
                    .blur(blurRadius)
                    .offset(y = verticalOffset)
                    .graphicsLayer { this.alpha = alpha },
            )
        }
    }
}
