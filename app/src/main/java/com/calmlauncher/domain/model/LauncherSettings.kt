package com.calmlauncher.domain.model

/** The three friction tiers. Each escalates delay and intent requirements. */
enum class FrictionLevel { LIGHT, MEDIUM, HARDCORE }

/** Context presets that re-shape visibility and blocking. */
enum class EnvironmentMode { NONE, WORK, STUDY, DEEP_WORK, SLEEP, GYM, OUTSIDE, TRAVEL }

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
    val defaultOpenDelaySeconds: Int = 0,

    // Display
    val displayMode: AppDisplayMode = AppDisplayMode.TEXT,
    val showRecents: Boolean = false,
    val showSuggestions: Boolean = false,

    // Anti-distraction toggles (map 1:1 to MODE_COVERAGE_TODO)
    val openingDelaysEnabled: Boolean = false,
    val intentPromptEnabled: Boolean = false,
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
    val themePreference: ThemePreference = ThemePreference.DARK,

    // App-limit extension budget.
    //
    // These are user-configurable but deliberately awkward to abuse. A *lower* value applies at
    // once, because tightening your own limits should never be blocked. A *higher* value is parked
    // in the `pending` fields and only becomes effective at the next daily reset — otherwise, when
    // an app is blocked, "raise the cap" would simply be a slower version of the unlimited
    // "Add 10 minutes" button this replaced. Absolute ceilings live in
    // [com.calmlauncher.domain.model.AppLimitCeilings] and no setting can exceed them.
    val limitExtensionsPerDay: Int = 2,
    val limitExtraMinutesPerDay: Int = 20,
    val limitMinutesPerExtension: Int = 10,

    /** A raised extensions cap awaiting the next daily reset. -1 when nothing is pending. */
    val pendingLimitExtensionsPerDay: Int = -1,

    /** A raised extra-minutes cap awaiting the next daily reset. -1 when nothing is pending. */
    val pendingLimitExtraMinutesPerDay: Int = -1,

    /** Day-start on which the pending caps were requested; they apply on any later day. */
    val limitCapsPendingSinceDayStartEpochMs: Long = 0L,

    // Analytics
    val collectUsageAnalyticsEnabled: Boolean = true,
    val analyticsRetentionDays: Int = 365,

    // Security
    val kioskModeEnabled: Boolean = false,
    val hideStatusBar: Boolean = false,

    // Focus session
    val focusActive: Boolean = false,
    val focusStartedAtEpochMs: Long = 0L,
    val focusDurationMinutes: Int = 25,

    // Favourites shown on the home screen, ordered (package names)
    val favorites: List<String> = DEFAULT_FAVORITES,

    /**
     * True once the Phone/Messages/Camera starter shortcuts have been seeded. Without this
     * flag a device refresh re-seeds them every time the user empties the list, making the
     * default favourites impossible to remove.
     */
    val favoritesSeeded: Boolean = false,

    // Onboarding
    val onboardingComplete: Boolean = false,
) {
    companion object {
        val DEFAULT_FAVORITES = listOf("phone", "messages", "camera")
    }
}
