package com.calmlauncher.domain.model

/** The three friction tiers. Each escalates delay and intent requirements. */
enum class FrictionLevel { LIGHT, MONK, HARDCORE }

/** Context presets that re-shape visibility and blocking. */
enum class EnvironmentMode { NONE, STUDY, SLEEP, GYM, DEEP_WORK, OUTSIDE }

/** How the app list renders entries. */
enum class AppDisplayMode { TEXT, ICONS }

/**
 * The complete, persisted preference set, surfaced as an immutable snapshot from the
 * [com.calmlauncher.domain.repository.SettingsRepository]. Everything the ModeEngine
 * and screens need to decide friction/visibility lives here.
 */
data class LauncherSettings(
    // Friction & environment
    val frictionLevel: FrictionLevel = FrictionLevel.LIGHT,
    val environmentMode: EnvironmentMode = EnvironmentMode.NONE,
    val defaultOpenDelaySeconds: Int = 2,

    // Display
    val displayMode: AppDisplayMode = AppDisplayMode.TEXT,
    val showRecents: Boolean = false,
    val showSuggestions: Boolean = false,

    // Anti-distraction toggles (map 1:1 to MODE_COVERAGE_TODO)
    val openingDelaysEnabled: Boolean = true,
    val intentPromptEnabled: Boolean = true,
    val breathUnlockEnabled: Boolean = false,
    val slowModeEnabled: Boolean = false,
    val analogModeEnabled: Boolean = false,
    val hideSocialApps: Boolean = true,
    val oneAppAtATime: Boolean = false,
    val dopamineDetectionEnabled: Boolean = true,
    val dynamicMinimalismEnabled: Boolean = true,
    val rewardRealLifeEnabled: Boolean = true,
    val recoveryModeEnabled: Boolean = true,
    val deadEndFeedsEnabled: Boolean = true,
    val einkSimulationEnabled: Boolean = false,
    val grayscaleEnabled: Boolean = false,

    // Security
    val pinEnabled: Boolean = false,
    val pinHash: String? = null,
    val kioskModeEnabled: Boolean = false,
    val hideStatusBar: Boolean = false,

    // Focus session
    val focusActive: Boolean = false,
    val focusStartedAtEpochMs: Long = 0L,
    val focusDurationMinutes: Int = 25,

    // Favourites shown on the home screen, ordered (package names)
    val favorites: List<String> = DEFAULT_FAVORITES,

    // Onboarding
    val onboardingComplete: Boolean = false,
) {
    companion object {
        val DEFAULT_FAVORITES = listOf("phone", "messages", "camera")
    }
}
