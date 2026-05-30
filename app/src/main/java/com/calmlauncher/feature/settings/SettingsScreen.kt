package com.calmlauncher.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.grayscale
import com.calmlauncher.domain.model.AppDisplayMode
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing

/**
 * The Settings hub: a focused (no bottom-nav) sub-page fronted by a [CalmBackBar]. A big
 * "Configuration" headline opens a single scrolling Column of [SettingRow]s — navigational
 * rows (value or chevron) at the top, then grouped toggle sections (Display / Friction /
 * Modes). Each toggle writes straight back through [SettingsViewModel.update]. The whole
 * surface desaturates when the active restriction enforces grayscale.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenManageApps: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenFriction: () -> Unit,
    onOpenEnvironment: () -> Unit,
    onOpenPin: () -> Unit,
    onOpenReflection: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val screenTimeText by viewModel.screenTimeText.collectAsStateWithLifecycle()
    val restriction by viewModel.restriction.collectAsStateWithLifecycle()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Settings", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .grayscale(restriction.grayscale, restriction.grayscaleAmount)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Configuration",
                style = CalmType.headlineLgMobile,
                color = CalmWhite,
                modifier = Modifier.padding(Spacing.marginMobile),
            )

            // --- Navigational rows -------------------------------------------------------
            SettingRow("Manage Apps", onClick = onOpenManageApps, showChevron = true)
            SettingRow("Screen Time", value = screenTimeText, onClick = onOpenScreenTime)
            SettingRow("Reflection", onClick = onOpenReflection, showChevron = true)
            SettingRow(
                title = "Friction Level",
                value = settings.frictionLevel.name.lowercase()
                    .replaceFirstChar { it.uppercase() },
                onClick = onOpenFriction,
            )
            SettingRow(
                title = "Environment",
                value = settings.environmentMode.displayName(),
                onClick = onOpenEnvironment,
            )
            SettingRow(
                title = "PIN Protection",
                value = if (settings.pinEnabled) "Enabled" else "Disabled",
                onClick = onOpenPin,
            )

            // --- Display -----------------------------------------------------------------
            SectionLabel("Display")
            SettingRow(
                title = "App Icons",
                trailing = {
                    CalmToggle(settings.displayMode == AppDisplayMode.ICONS) { checked ->
                        viewModel.update {
                            it.copy(
                                displayMode = if (checked) AppDisplayMode.ICONS else AppDisplayMode.TEXT,
                            )
                        }
                    }
                },
            )
            SettingRow(
                title = "Greyscale Mode",
                trailing = {
                    CalmToggle(settings.grayscaleEnabled) { checked ->
                        viewModel.update { it.copy(grayscaleEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "E-Ink Simulation",
                trailing = {
                    CalmToggle(settings.einkSimulationEnabled) { checked ->
                        viewModel.update { it.copy(einkSimulationEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Show Recents",
                trailing = {
                    CalmToggle(settings.showRecents) { checked ->
                        viewModel.update { it.copy(showRecents = checked) }
                    }
                },
            )
            SettingRow(
                title = "Show Suggestions",
                trailing = {
                    CalmToggle(settings.showSuggestions) { checked ->
                        viewModel.update { it.copy(showSuggestions = checked) }
                    }
                },
            )

            // --- Friction ----------------------------------------------------------------
            SectionLabel("Friction")
            SettingRow(
                title = "Opening Delays",
                trailing = {
                    CalmToggle(settings.openingDelaysEnabled) { checked ->
                        viewModel.update { it.copy(openingDelaysEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Intent Prompt",
                trailing = {
                    CalmToggle(settings.intentPromptEnabled) { checked ->
                        viewModel.update { it.copy(intentPromptEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Breath Unlock",
                trailing = {
                    CalmToggle(settings.breathUnlockEnabled) { checked ->
                        viewModel.update { it.copy(breathUnlockEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Slow Mode",
                trailing = {
                    CalmToggle(settings.slowModeEnabled) { checked ->
                        viewModel.update { it.copy(slowModeEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Analog Mode",
                trailing = {
                    CalmToggle(settings.analogModeEnabled) { checked ->
                        viewModel.update { it.copy(analogModeEnabled = checked) }
                    }
                },
            )

            // --- Modes -------------------------------------------------------------------
            SectionLabel("Modes")
            SettingRow(
                title = "Hide Social Apps",
                trailing = {
                    CalmToggle(settings.hideSocialApps) { checked ->
                        viewModel.update { it.copy(hideSocialApps = checked) }
                    }
                },
            )
            SettingRow(
                title = "One App At A Time",
                trailing = {
                    CalmToggle(settings.oneAppAtATime) { checked ->
                        viewModel.update { it.copy(oneAppAtATime = checked) }
                    }
                },
            )
            SettingRow(
                title = "Dopamine Detection",
                trailing = {
                    CalmToggle(settings.dopamineDetectionEnabled) { checked ->
                        viewModel.update { it.copy(dopamineDetectionEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Dynamic Minimalism",
                trailing = {
                    CalmToggle(settings.dynamicMinimalismEnabled) { checked ->
                        viewModel.update { it.copy(dynamicMinimalismEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Recovery Mode",
                trailing = {
                    CalmToggle(settings.recoveryModeEnabled) { checked ->
                        viewModel.update { it.copy(recoveryModeEnabled = checked) }
                    }
                },
            )
            SettingRow(
                title = "Dead-End Feeds",
                trailing = {
                    CalmToggle(settings.deadEndFeedsEnabled) { checked ->
                        viewModel.update { it.copy(deadEndFeedsEnabled = checked) }
                    }
                },
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}
