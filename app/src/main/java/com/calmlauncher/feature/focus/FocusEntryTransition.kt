package com.calmlauncher.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import kotlinx.coroutines.delay

@Composable
fun FocusEntryTransition(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealedLetters by remember { mutableIntStateOf(0) }
    var entered by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {}

    LaunchedEffect(Unit) {
        "FOCUS".forEachIndexed { index, _ ->
            delay(if (index == 0) 120L else 180L)
            revealedLetters = index + 1
        }
        delay(650L)
        entered = true
    }

    if (entered) {
        FocusScreen(onExit = onExit, modifier = modifier)
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CalmBlack),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(horizontal = 2.dp),
            ) {
                "FOCUS".forEachIndexed { index, letter ->
                    AnimatedVisibility(
                        visible = index < revealedLetters,
                        enter = fadeIn(tween(durationMillis = 420)) +
                            slideInVertically(
                                animationSpec = tween(durationMillis = 520),
                                initialOffsetY = { it / 2 },
                            ),
                    ) {
                        Text(
                            text = letter.toString(),
                            style = CalmType.heroTime,
                            color = CalmWhite,
                            modifier = Modifier.graphicsLayer {
                                alpha = 0.98f
                            },
                        )
                    }
                }
            }
        }
    }
}
