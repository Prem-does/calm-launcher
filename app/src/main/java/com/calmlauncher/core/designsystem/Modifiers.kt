package com.calmlauncher.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Desaturates everything drawn by the receiver subtree. Used to enforce grayscale
 * inside the launcher (system-wide grayscale needs OS privileges — see
 * PlatformGuardPolicy). [amount] of 1f is fully gray, 0f is untouched.
 */
fun Modifier.grayscale(enabled: Boolean, amount: Float = 1f): Modifier {
    if (!enabled || amount <= 0f) return this
    val saturation = (1f - amount).coerceIn(0f, 1f)
    return this.then(
        Modifier.drawWithCache {
            val matrix = ColorMatrix().apply { setToSaturation(saturation) }
            val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) }
            onDrawWithContent {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(
                        bounds = androidx.compose.ui.geometry.Rect(Offset.Zero, size),
                        paint = paint,
                    )
                    drawContent()
                    canvas.restore()
                }
            }
        },
    )
}
