package com.calmlauncher.feature.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.EInkBackdrop
import com.calmlauncher.core.designsystem.component.HoldToConfirm
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.feature.reflection.ReflectionField
import com.calmlauncher.feature.reflection.ReflectionViewModel

/** Opacity of the deliberately de-emphasised time strip at the very top of the canvas. */
private const val DimTimeAlpha = 0.2f

/**
 * Focus Mode: a pure-black, single-purpose canvas built to hold attention. On enter it
 * starts a session (if one isn't already running) so quick-exit apps stay hard-blocked.
 * A faint e-ink texture sits underneath; the day's calming quote is centered; and the only
 * way out is a deliberate hold on the bottom "End Focus" control, which fills a thin
 * progress bar before confirming. The whole surface desaturates when the restriction state
 * enforces grayscale. There is no bottom navigation here by design.
 *
 * @param onExit invoked once the user completes the hold-to-exit gesture.
 */
@Composable
fun FocusScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reflectionViewModel: ReflectionViewModel = hiltViewModel()
    val reflectionState by reflectionViewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = true) {}
    LaunchedEffect(Unit) { viewModel.startIfNeeded() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalmBlack)
    ) {
        // Bottom layer: barely-perceptible e-ink speckle, never intercepts input. Only drawn
        // while the E-Ink setting is on — turning it off leaves a plain black canvas.
        if (state.einkTexture) {
            EInkBackdrop(Modifier.matchParentSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Centered, day-stable quote — the sole focal point.
            Text(
                text = state.quote,
                style = CalmType.headlineMd,
                color = CalmWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile),
            )

            // Nightly reflection prompt and note field, available directly in Focus Mode.
            if (reflectionState.prompt.isNotBlank()) {
                Text(
                    text = reflectionState.prompt,
                    style = CalmType.bodyMd,
                    color = CalmWhite.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.marginMobile)
                        .padding(top = Spacing.stackLg),
                )
            }

            ReflectionField(
                value = reflectionState.response,
                onValueChange = reflectionViewModel::onResponseChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile)
                    .padding(top = Spacing.gutter),
            )

            CalmButton(
                text = "Save reflection",
                onClick = reflectionViewModel::save,
                style = CalmButtonStyle.Filled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile)
                    .padding(top = Spacing.gutter),
            )
            if (reflectionState.saveStatusText.isNotBlank()) {
                Text(
                    text = reflectionState.saveStatusText,
                    style = CalmType.labelMd,
                    color = CalmWhite.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.marginMobile)
                        .padding(top = Spacing.stackSm),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.stackLg))

            // Deliberate hold-to-exit control with its own progress bar + sub-label.
            HoldToConfirm(
                label = "End Focus",
                subLabel = "Hold to exit",
                holdMillis = 3000L,
                onConfirm = {
                    reflectionViewModel.save()
                    viewModel.endFocus()
                    onExit()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.stackLg),
            )
        }
    }
}
