package com.calmlauncher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.LauncherSettings

@Composable
fun EnvironmentScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val selected = settings.environmentMode
    val selectedEffect = selected.effect()

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Environment", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel("Preset")
            EnvironmentMode.entries.forEach { mode ->
                EnvironmentRow(
                    title = mode.displayName(),
                    effect = mode.effect(),
                    selected = mode == selected,
                    // Selecting a preset applies it straight away. Picking None resets every
                    // setting a preset can touch, so no environment character survives it.
                    onClick = { viewModel.update { current -> current.applyEnvironmentSetup(mode) } },
                )
            }

            SectionLabel("Selected Setup")
            SelectedSetupBlock(
                mode = selected,
                effect = selectedEffect,
                settings = settings,
                onCycleFocusLength = {
                    viewModel.update { current ->
                        current.copy(focusDurationMinutes = current.focusDurationMinutes.nextFocusDuration())
                    }
                },
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}

@Composable
private fun EnvironmentRow(
    title: String,
    effect: EnvironmentEffect,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val background = if (selected) CalmWhite else CalmBlack
    val titleColor = if (selected) CalmBlack else CalmWhite
    val bodyColor = if (selected) CalmBlack else CalmGray
    val labelColor = if (selected) CalmBlack else CalmGrayDim

    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                )
                .background(background)
                .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
            verticalArrangement = Arrangement.spacedBy(Spacing.stackSm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, style = CalmType.bodyLg, color = titleColor, modifier = Modifier.weight(1f))
                if (selected) {
                    Text(text = "Active", style = CalmType.labelMd, color = CalmBlack)
                }
            }
            Text(text = effect.description, style = CalmType.bodyMd, color = bodyColor)
            EffectLine(label = "Easy", value = effect.easy, labelColor = labelColor, valueColor = bodyColor)
            EffectLine(label = "Blocks", value = effect.blocks, labelColor = labelColor, valueColor = bodyColor)
            if (effect.focusBlocks.isNotBlank()) {
                EffectLine(label = "Focus", value = effect.focusBlocks, labelColor = labelColor, valueColor = bodyColor)
            }
        }
        ThinDivider()
    }
}

@Composable
private fun EffectLine(
    label: String,
    value: String,
    labelColor: androidx.compose.ui.graphics.Color,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
    ) {
        Text(text = label, style = CalmType.labelMd, color = labelColor)
        Text(text = value, style = CalmType.bodyMd, color = valueColor, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SelectedSetupBlock(
    mode: EnvironmentMode,
    effect: EnvironmentEffect,
    settings: LauncherSettings,
    onCycleFocusLength: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.gutter),
    ) {
        Text(text = effect.setup, style = CalmType.bodyMd, color = CalmGray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCycleFocusLength,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Focus Length", style = CalmType.bodyLg, color = CalmWhite, modifier = Modifier.weight(1f))
            Text(text = "${settings.focusDurationMinutes}m", style = CalmType.bodyMd, color = CalmGray)
        }
        Text(
            text = if (mode == EnvironmentMode.NONE) {
                "No preset is active. Blocking, greyscale and friction helpers are back to your baseline."
            } else {
                "${mode.displayName()} is active now. Choose None to clear it."
            },
            style = CalmType.labelMd,
            color = CalmGrayDim,
        )
    }
    ThinDivider()
}

private data class EnvironmentEffect(
    val description: String,
    val easy: String,
    val blocks: String,
    val focusBlocks: String = "",
    val setup: String,
)

private fun EnvironmentMode.effect(): EnvironmentEffect = when (this) {
    EnvironmentMode.NONE -> EnvironmentEffect(
        description = "Default launcher behavior without a context preset.",
        easy = "Your normal favorites and app list",
        blocks = "Nothing extra",
        setup = "Clears every preset change — blocking, greyscale, e-ink and friction helpers all return to your baseline.",
    )
    EnvironmentMode.WORK -> EnvironmentEffect(
        description = "Keeps work tools available while obvious drift stays out of the way.",
        easy = "Tools, communication, browser, store",
        blocks = "Social, entertainment, games",
        setup = "Hides recents and suggestions, keeps social apps tucked away, and uses a 45m focus length.",
    )
    EnvironmentMode.STUDY -> EnvironmentEffect(
        description = "For reading, notes, research, and classwork.",
        easy = "Tools, notes, browser, communication",
        blocks = "Social, entertainment, games",
        setup = "Turns on basic friction helpers and uses a 50m focus length.",
    )
    EnvironmentMode.DEEP_WORK -> EnvironmentEffect(
        description = "A stricter preset for single-task sessions.",
        easy = "Tools and direct communication",
        blocks = "Social, entertainment, browser, store, games",
        setup = "Hides noisy surfaces, enables stronger friction helpers, and uses a 90m focus length.",
    )
    EnvironmentMode.SLEEP -> EnvironmentEffect(
        description = "A quiet wind-down mode for late hours.",
        easy = "Phone, messages, alarms, essentials",
        blocks = "Social, entertainment, browser, games",
        setup = "Enables grayscale and e-ink posture, adds opening delays, hides noisy surfaces, and uses a short 15m focus length.",
    )
    EnvironmentMode.GYM -> EnvironmentEffect(
        description = "Keeps workout utilities close without over-locking the phone.",
        easy = "Music, timers, health, communication",
        blocks = "Nothing extra",
        focusBlocks = "Social and games during focus",
        setup = "Keeps the launcher light, hides noisy surfaces, and uses a 45m focus length.",
    )
    EnvironmentMode.OUTSIDE -> EnvironmentEffect(
        description = "For errands and walks, where the phone should stay practical.",
        easy = "Maps, camera, phone, messages",
        blocks = "Nothing extra",
        focusBlocks = "Social and games during focus",
        setup = "Keeps practical tools reachable, hides noisy surfaces, and uses a 30m focus length.",
    )
    EnvironmentMode.TRAVEL -> EnvironmentEffect(
        description = "For moving through the world: navigation, tickets, and communication first.",
        easy = "Maps, browser, camera, store, communication",
        blocks = "Nothing extra",
        focusBlocks = "Social and games during focus",
        setup = "Keeps navigation-friendly apps reachable, hides noisy surfaces, and uses a 30m focus length.",
    )
}

/**
 * Apply a preset's posture to the settings snapshot.
 *
 * [EnvironmentMode.NONE] is the inverse operation: it must clear **every** field any other
 * preset can write, otherwise leftovers (hidden social apps, grayscale, opening delays)
 * survive after the user goes back to no environment at all.
 */
private fun LauncherSettings.applyEnvironmentSetup(mode: EnvironmentMode): LauncherSettings = when (mode) {
    EnvironmentMode.NONE -> copy(
        environmentMode = mode,
        hideSocialApps = false,
        openingDelaysEnabled = false,
        intentPromptEnabled = false,
        slowModeEnabled = false,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        showRecents = false,
        showSuggestions = false,
        focusDurationMinutes = 25,
    )
    // Every preset below writes the same full field set, so switching between presets never
    // leaves a previous one's helpers (slow mode, greyscale, delays) silently switched on.
    EnvironmentMode.WORK -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = true,
        intentPromptEnabled = true,
        slowModeEnabled = false,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        focusDurationMinutes = 45,
    )
    EnvironmentMode.STUDY -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = true,
        intentPromptEnabled = true,
        slowModeEnabled = false,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        focusDurationMinutes = 50,
    )
    EnvironmentMode.DEEP_WORK -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = true,
        intentPromptEnabled = true,
        slowModeEnabled = true,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        focusDurationMinutes = 90,
    )
    EnvironmentMode.SLEEP -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = true,
        intentPromptEnabled = false,
        slowModeEnabled = false,
        grayscaleEnabled = true,
        einkSimulationEnabled = true,
        focusDurationMinutes = 15,
    )
    EnvironmentMode.GYM -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = false,
        intentPromptEnabled = false,
        slowModeEnabled = false,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        focusDurationMinutes = 45,
    )
    EnvironmentMode.OUTSIDE -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = false,
        intentPromptEnabled = false,
        slowModeEnabled = false,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        focusDurationMinutes = 30,
    )
    EnvironmentMode.TRAVEL -> copy(
        environmentMode = mode,
        hideSocialApps = true,
        showRecents = false,
        showSuggestions = false,
        openingDelaysEnabled = false,
        intentPromptEnabled = false,
        slowModeEnabled = false,
        grayscaleEnabled = false,
        einkSimulationEnabled = false,
        focusDurationMinutes = 30,
    )
}

private fun Int.nextFocusDuration(): Int = when {
    this < 25 -> 25
    this < 30 -> 30
    this < 45 -> 45
    this < 50 -> 50
    this < 90 -> 90
    else -> 15
}
