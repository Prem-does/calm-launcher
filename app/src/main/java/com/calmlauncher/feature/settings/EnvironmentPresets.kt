package com.calmlauncher.feature.settings

import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.LauncherSettings

/**
 * Applies one environment preset to the persisted settings snapshot.
 *
 * Keeping this in a shared file lets both the Environment settings screen and the Home
 * quick switcher use the exact same preset mapping.
 */
fun LauncherSettings.applyEnvironmentSetup(mode: EnvironmentMode): LauncherSettings = when (mode) {
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