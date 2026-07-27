package com.calmlauncher.feature.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.ConfirmDialog
import com.calmlauncher.core.designsystem.component.CountdownOverlay
import com.calmlauncher.core.designsystem.component.ReasonPrompt
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.domain.model.FrictionStep
import com.calmlauncher.launcher.LaunchEffect

/**
 * The full-screen launch gate. Renders nothing while idle, and overlays the current
 * [FrictionStep] in a pure-black surface above the rest of the app whenever the
 * [com.calmlauncher.launcher.LaunchCoordinator] has a flow in progress. It reacts to
 * one-shot effects: a dead-end routes to the reset screen, and either kind of block —
 * a mode-engine refusal or an exhausted app limit — lands on a [BlockCountdownOverlay]
 * the user has to sit through. Drop this at the top of the navigation host's content so
 * it can cover any screen.
 */
@Composable
fun LaunchGateHost(
    onNavigateToReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GateViewModel = hiltViewModel(),
) {
    val state by viewModel.flow.collectAsStateWithLifecycle()
    var blocked by remember { mutableStateOf<LaunchEffect.Blocked?>(null) }
    var appLimitBlock by remember { mutableStateOf<LaunchEffect.AppLimitBlocked?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { eff ->
            when (eff) {
                is LaunchEffect.DeadEnd -> onNavigateToReset()
                is LaunchEffect.Blocked -> blocked = eff
                is LaunchEffect.AppLimitBlocked -> appLimitBlock = eff
                else -> {}
            }
        }
    }

    val current = state?.current
    when {
        appLimitBlock != null -> {
            val limit = appLimitBlock!!
            val status = limit.status
            BlockCountdownOverlay(
                seconds = LimitCountdownSeconds,
                title = "Limit reached",
                appLabel = status.label,
                detail = "${status.usedMinutes}m used today of ${status.dailyLimitMinutes ?: 0}m.",
                overrideLabel = if (status.canGrantOverride) "Add 10 minutes" else null,
                onOverride = {
                    viewModel.coordinator.grantAppLimitOverrideAndLaunch(limit.request, 10)
                    appLimitBlock = null
                },
                onDismiss = { appLimitBlock = null },
                footnote = if (status.canGrantOverride) {
                    null
                } else {
                    "Both 10-minute extensions are used up for today."
                },
                modifier = modifier,
            )
        }

        // A mode-engine block (focus session / environment preset). There is no override to
        // offer, so the countdown is simply the pause before returning to the launcher.
        blocked != null -> {
            val block = blocked!!
            BlockCountdownOverlay(
                seconds = BlockCountdownSeconds,
                title = "Blocked",
                appLabel = block.request.label,
                detail = block.reason,
                onDismiss = { blocked = null },
                modifier = modifier,
            )
        }

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
    }
}
