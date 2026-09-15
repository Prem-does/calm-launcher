package com.calmlauncher.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite

@Composable
fun FocusEntryTransition(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    val letters = remember { "focus".map { LetterAnimation() } }

    BackHandler(enabled = true) {}

    LaunchedEffect(Unit) {
        coroutineScope {
            letters.forEachIndexed { index, letter ->
                launch {
                    delay(index * LetterDelayMillis)
                    coroutineScope {
                        launch {
                            letter.alpha.animateTo(
                                targetValue = 1f,
                                animationSpec = letterKeyframes {
                                    0f at 0
                                    0.5f at 350
                                    1f at 700
                                },
                            )
                        }
                        launch {
                            letter.blurRadius.animateTo(
                                targetValue = 0f,
                                animationSpec = letterKeyframes {
                                    10f at 0
                                    5f at 350
                                    0f at 700
                                },
                            )
                        }
                        launch {
                            letter.verticalOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = letterKeyframes {
                                    -50f at 0
                                    5f at 350
                                    0f at 700
                                },
                            )
                        }
                    }
                }
            }
        }
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
            Row {
                "focus".forEachIndexed { index, letter ->
                    val animation = letters[index]
                    Text(
                        text = letter.toString(),
                        style = CalmType.heroTime,
                        color = CalmWhite,
                        modifier = Modifier
                            .blur(animation.blurRadius.value.dp)
                            .offset(y = animation.verticalOffset.value.dp)
                            .graphicsLayer { this.alpha = animation.alpha.value },
                    )
                }
            }
        }
    }
}

private class LetterAnimation {
    val alpha = Animatable(0f)
    val blurRadius = Animatable(10f)
    val verticalOffset = Animatable(-50f)
}

private fun letterKeyframes(block: KeyframesSpec.KeyframesSpecConfig<Float>.() -> Unit) =
    keyframes {
        durationMillis = LetterAnimationDurationMillis
        this.block()
    }

private const val LetterDelayMillis = 200L
private const val LetterAnimationDurationMillis = 700
