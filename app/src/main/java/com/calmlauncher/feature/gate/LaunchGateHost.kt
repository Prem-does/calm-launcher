package com.calmlauncher.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.BreathOverlay
import com.calmlauncher.core.designsystem.component.ConfirmDialog
import com.calmlauncher.core.designsystem.component.CountdownOverlay
import com.calmlauncher.core.designsystem.component.ReasonPrompt
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.FrictionStep
import com.calmlauncher.launcher.LaunchEffect
import kotlinx.coroutines.delay

/** How long a transient "blocked" message lingers before it gently clears. */
private const val BlockedMessageMs = 2500L

/**
 * The full-screen launch gate. Renders nothing while idle, and overlays the current
 * [FrictionStep] in a pure-black surface above the rest of the app whenever the
 * [LaunchCoordinator] has a flow in progress. It reacts to one-shot effects: a
 * dead-end routes to the reset screen, and a block surfaces a brief calming message
 * that auto-dismisses. Drop this at the top of the navigation host's content so it can
 * cover any screen.
 */
@Composable
fun LaunchGateHost(
    onNavigateToReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GateViewModel = hiltViewModel(),
) {
    val state by viewModel.flow.collectAsStateWithLifecycle()
    var blockedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { eff ->
            when (eff) {
                is LaunchEffect.DeadEnd -> onNavigateToReset()
                is LaunchEffect.Blocked -> blockedMessage = eff.reason
                else -> {}
            }
        }
    }

    // Clear the transient blocked message after a calm beat.
    LaunchedEffect(blockedMessage) {
        if (blockedMessage != null) {
            delay(BlockedMessageMs)
            blockedMessage = null
        }
    }

    val current = state?.current
    when {
        // An active friction step is showing — render it full-screen over everything.
        state != null && current != null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(CalmBlack),
            ) {
                when (val step = current) {
                    is FrictionStep.Reason -> ReasonPrompt(
                        appLabel = state!!.request.label,
                        onSubmit = viewModel::submitReason,
                        onCancel = viewModel::cancel,
                    )

                    is FrictionStep.Breath -> BreathOverlay(
                        cycles = step.cycles,
                        onComplete = viewModel::stepComplete,
                        onCancel = viewModel::cancel,
                    )

                    is FrictionStep.Delay -> CountdownOverlay(
                        seconds = step.seconds,
                        appLabel = state!!.request.label,
                        onFinished = viewModel::stepComplete,
                        onCancel = viewModel::cancel,
                    )

                    is FrictionStep.Confirm -> ConfirmDialog(
                        message = step.message,
                        onConfirm = viewModel::stepComplete,
                        onDismiss = viewModel::cancel,
                    )

                    // DeadEnd / Block are handled as effects by the coordinator, never
                    // surfaced here as in-flow steps.
                    else -> {}
                }
            }
        }

        // No active gate — optionally show a brief, self-dismissing blocked message.
        blockedMessage != null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(CalmBlack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = blockedMessage!!,
                    style = CalmType.bodyLg,
                    color = CalmWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .systemBarsPadding()
                        .padding(horizontal = Spacing.marginMobile),
                )
            }
        }
    }
}
