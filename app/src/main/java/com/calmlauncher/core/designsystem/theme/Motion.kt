package com.calmlauncher.core.designsystem.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * Motion is limited to a single subtle opacity fade. No sliding, scaling, or spring
 * physics — this preserves the e-ink, low-stimulation feel. When E-Ink simulation /
 * low-motion is active the duration collapses to zero (see [fadeDuration]).
 */
object CalmMotion {
    const val FADE_MS = 200
    const val FADE_MS_NONE = 0

    fun fadeDuration(motionEnabled: Boolean): Int =
        if (motionEnabled) FADE_MS else FADE_MS_NONE

    fun enter(motionEnabled: Boolean = true) =
        fadeIn(animationSpec = tween(durationMillis = fadeDuration(motionEnabled), easing = LinearEasing))

    fun exit(motionEnabled: Boolean = true) =
        fadeOut(animationSpec = tween(durationMillis = fadeDuration(motionEnabled), easing = LinearEasing))
}
