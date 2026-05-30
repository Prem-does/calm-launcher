package com.calmlauncher.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AppDisplayMode

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

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Settings", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    val context = LocalContext.current
                    CalmToggle(settings.grayscaleEnabled) { checked ->
                        viewModel.update { it.copy(grayscaleEnabled = checked) }
                        if (!openColorCorrectionSettings(context)) {
                            Toast.makeText(context, "Unable to open Colour Correction settings.", Toast.LENGTH_SHORT).show()
                            return@CalmToggle
                        }

                        Toast.makeText(
                            context,
                            if (checked) {
                                "Enable Colour Correction and select Greyscale."
                            } else {
                                "Disable Colour Correction to return to normal colours."
                            },
                            Toast.LENGTH_LONG,
                        ).show()
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

private fun openColorCorrectionSettings(context: android.content.Context): Boolean {
    val pm = context.packageManager

    val candidates = listOf(
        Intent("com.android.settings.ACCESSIBILITY_COLOR_SPACE_SETTINGS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        Intent(Intent.ACTION_MAIN)
            .setComponent(
                ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$AccessibilityDaltonizerSettingsActivity",
                ),
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    for (intent in candidates) {
        if (pm.resolveActivity(intent, 0) != null) {
            context.startActivity(intent)
            return true
        }
    }

    return false
}
