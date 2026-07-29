package com.calmlauncher.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmButton
import com.calmlauncher.core.designsystem.component.CalmButtonStyle
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/** Trailing value shown on a step row once its system surface reports satisfied. */
private const val StepDone = "Done"

/** Trailing value prompting the user to open the relevant system surface. */
private const val StepSetUp = "Set up"

private fun stepValue(done: Boolean) = if (done) StepDone else StepSetUp

/**
 * First-run onboarding: a calm, pure-black walkthrough of the system permissions Calm
 * wants before it takes over the home screen. Each step is a tappable [SettingRow] that
 * deep-links into the matching OS surface and shows "Done" / "Set up" based on the live
 * status. The battery-exemption step is called out because Samsung One UI aggressively
 * sleeps apps, which would otherwise kill the focus / grayscale services.
 *
 * Statuses are re-pulled via [OnboardingViewModel.refreshStatus] every time the screen
 * resumes, so they update the moment the user returns from a settings screen. None of the
 * steps are strictly mandatory — the "Begin" button is always enabled.
 *
 * @param onComplete invoked after the onboarding flag is persisted and work scheduled.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-read system permission states each time we come back to the foreground (e.g.
    // returning from a system settings deep-link), so the rows reflect reality.
    LifecycleResumeEffect(Unit) {
        viewModel.refreshStatus()
        onPauseOrDispose { }
    }

    CalmScaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Calm headline + body.
            Text(
                text = "Make your phone quiet.",
                style = CalmType.headlineLgMobile,
                color = CalmWhite,
                modifier = Modifier.padding(
                    start = Spacing.marginMobile,
                    end = Spacing.marginMobile,
                    top = Spacing.stackLg,
                    bottom = Spacing.stackMd,
                ),
            )
            Text(
                text = "A few quick steps so Calm can stay out of your way — and stay awake.",
                style = CalmType.bodyLg,
                color = CalmGray,
                modifier = Modifier.padding(
                    start = Spacing.marginMobile,
                    end = Spacing.marginMobile,
                    bottom = Spacing.stackLg,
                ),
            )

            // Permission steps. Each deep-links to its OS surface; status is re-pulled on
            // resume so the trailing value flips to "Done" automatically.
            SettingRow(
                title = "Set as default home",
                value = stepValue(state.isDefaultHome),
                onClick = {
                    runCatching {
                        context.startActivity(viewModel.homeRoleManager.requestDefaultHomeIntent())
                    }
                },
            )
            SettingRow(
                title = "Allow screen-time access",
                value = stepValue(state.hasUsageAccess),
                onClick = { viewModel.openUsageAccess() },
            )
            SettingRow(
                title = "Keep Calm awake (battery)",
                value = stepValue(state.batteryExempt),
                onClick = { viewModel.requestBattery() },
            )
            Text(
                text = "Samsung phones aggressively sleep apps. Allowing this keeps Calm's " +
                    "focus and grayscale features from being shut down in the background.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    start = Spacing.marginMobile,
                    end = Spacing.marginMobile,
                    top = Spacing.stackSm,
                    bottom = Spacing.stackMd,
                ),
            )
            SettingRow(
                title = "Enable Focus Guard (optional)",
                value = stepValue(state.focusServiceOn),
                onClick = { viewModel.openAccessibility() },
            )
            SettingRow(
                title = "Show block screens (optional)",
                value = stepValue(state.canDrawOverlays),
                onClick = { viewModel.openOverlayPermission() },
            )
            Text(
                text = "Without this, an app that runs out of time is simply closed. With it, " +
                    "Calm can explain why first — and offer you a few more minutes.",
                style = CalmType.labelMd,
                color = CalmGray,
                modifier = Modifier.padding(
                    start = Spacing.marginMobile,
                    end = Spacing.marginMobile,
                    top = Spacing.stackSm,
                    bottom = Spacing.stackMd,
                ),
            )

            Spacer(Modifier.height(Spacing.stackLg))

            // Always enabled — none of the steps are strictly required.
            CalmButton(
                text = "Begin",
                style = CalmButtonStyle.Filled,
                onClick = {
                    viewModel.complete()
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.marginMobile),
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}
