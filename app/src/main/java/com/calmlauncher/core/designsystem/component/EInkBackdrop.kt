package com.calmlauncher.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.calmlauncher.core.designsystem.theme.CalmGray
import kotlin.random.Random

/** Number of speckles scattered per backdrop — enough for texture, cheap to draw. */
private const val DotCount = 1400

/** Deterministic seed so the texture is stable across recompositions / redraws. */
private const val NoiseSeed = 0x5EED

/**
 * A barely-perceptible full-bleed "e-ink" texture for the Focus screen. Scatters many
 * tiny [CalmGray] dots at [opacity] (default 0.03f) via a single [drawBehind] pass —
 * cheaper than an image and resolution-independent. The speckle layout is seeded so it
 * doesn't shimmer between frames. Drop it as the bottom layer of a Box; it is nearly
 * invisible by design and never intercepts input.
 */
@Composable
fun EInkBackdrop(
    modifier: Modifier = Modifier,
    opacity: Float = 0.03f,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val random = Random(NoiseSeed)
                val dotColor = CalmGray.copy(alpha = opacity.coerceIn(0f, 1f))
                val radius = 0.6.dp.toPx()
                repeat(DotCount) {
                    val x = random.nextFloat() * size.width
                    val y = random.nextFloat() * size.height
                    drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                }
            },
    )
}
