package com.calmlauncher.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calmlauncher.domain.model.AppDisplayMode
import com.calmlauncher.domain.model.EnvironmentMode
import com.calmlauncher.domain.model.FrictionLevel
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes the full [LauncherSettings] snapshot from a Preferences [DataStore].
 * Every field maps to one key; enums are stored by [Enum.name]; the ordered favourites
 * list is stored as a newline-delimited string (package names can never contain '\n').
 *
 * Defaults always come from the [LauncherSettings] model so a freshly-installed app (or a
 * missing key after an upgrade) degrades to the documented defaults.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<LauncherSettings> = dataStore.data.map { it.toSettings() }

    suspend fun current(): LauncherSettings = settings.first()

    /** Read current snapshot, apply [transform], then write every key atomically. */
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            updated.writeInto(prefs)
        }
    }

    /** Remove stale keys from older settings schemas. Safe to call repeatedly. */
    suspend fun purgeLegacyKeys() {
        dataStore.edit { prefs ->
            prefs.remove(LegacyKeys.PIN_ENABLED)
            prefs.remove(LegacyKeys.PIN_HASH)
            prefs.remove(LegacyKeys.BREATH_UNLOCK_ENABLED)
        }
    }

    // -- Mapping -----------------------------------------------------------------

    private fun Preferences.toSettings(): LauncherSettings {
        val defaults = LauncherSettings()
        return LauncherSettings(
            frictionLevel = this[Keys.FRICTION_LEVEL].toFrictionLevel(defaults.frictionLevel),
            environmentMode = this[Keys.ENVIRONMENT_MODE].toEnvironmentMode(defaults.environmentMode),
            defaultOpenDelaySeconds = this[Keys.DEFAULT_OPEN_DELAY_SECONDS] ?: defaults.defaultOpenDelaySeconds,
            displayMode = this[Keys.DISPLAY_MODE].toDisplayMode(defaults.displayMode),
            showRecents = this[Keys.SHOW_RECENTS] ?: defaults.showRecents,
            showSuggestions = this[Keys.SHOW_SUGGESTIONS] ?: defaults.showSuggestions,
            openingDelaysEnabled = this[Keys.OPENING_DELAYS_ENABLED] ?: defaults.openingDelaysEnabled,
            intentPromptEnabled = this[Keys.INTENT_PROMPT_ENABLED] ?: defaults.intentPromptEnabled,
            slowModeEnabled = this[Keys.SLOW_MODE_ENABLED] ?: defaults.slowModeEnabled,
            analogModeEnabled = this[Keys.ANALOG_MODE_ENABLED] ?: defaults.analogModeEnabled,
            hideSocialApps = this[Keys.HIDE_SOCIAL_APPS] ?: defaults.hideSocialApps,
            oneAppAtATime = this[Keys.ONE_APP_AT_A_TIME] ?: defaults.oneAppAtATime,
            dopamineDetectionEnabled = this[Keys.DOPAMINE_DETECTION_ENABLED] ?: defaults.dopamineDetectionEnabled,
            dynamicMinimalismEnabled = this[Keys.DYNAMIC_MINIMALISM_ENABLED] ?: defaults.dynamicMinimalismEnabled,
            rewardRealLifeEnabled = this[Keys.REWARD_REAL_LIFE_ENABLED] ?: defaults.rewardRealLifeEnabled,
            recoveryModeEnabled = this[Keys.RECOVERY_MODE_ENABLED] ?: defaults.recoveryModeEnabled,
            deadEndFeedsEnabled = this[Keys.DEAD_END_FEEDS_ENABLED] ?: defaults.deadEndFeedsEnabled,
            einkSimulationEnabled = this[Keys.EINK_SIMULATION_ENABLED] ?: defaults.einkSimulationEnabled,
            grayscaleEnabled = this[Keys.GRAYSCALE_ENABLED] ?: defaults.grayscaleEnabled,
            themePreference = this[Keys.THEME_PREFERENCE].toThemePreference(defaults.themePreference),
            limitExtensionsPerDay = this[Keys.LIMIT_EXTENSIONS_PER_DAY] ?: defaults.limitExtensionsPerDay,
            limitExtraMinutesPerDay = this[Keys.LIMIT_EXTRA_MINUTES_PER_DAY] ?: defaults.limitExtraMinutesPerDay,
            limitMinutesPerExtension = this[Keys.LIMIT_MINUTES_PER_EXTENSION] ?: defaults.limitMinutesPerExtension,
            pendingLimitExtensionsPerDay = this[Keys.PENDING_LIMIT_EXTENSIONS_PER_DAY] ?: defaults.pendingLimitExtensionsPerDay,
            pendingLimitExtraMinutesPerDay = this[Keys.PENDING_LIMIT_EXTRA_MINUTES_PER_DAY] ?: defaults.pendingLimitExtraMinutesPerDay,
            limitCapsPendingSinceDayStartEpochMs = this[Keys.LIMIT_CAPS_PENDING_SINCE] ?: defaults.limitCapsPendingSinceDayStartEpochMs,
            collectUsageAnalyticsEnabled = this[Keys.COLLECT_USAGE_ANALYTICS] ?: defaults.collectUsageAnalyticsEnabled,
            analyticsRetentionDays = this[Keys.ANALYTICS_RETENTION_DAYS] ?: defaults.analyticsRetentionDays,
            kioskModeEnabled = this[Keys.KIOSK_MODE_ENABLED] ?: defaults.kioskModeEnabled,
            hideStatusBar = this[Keys.HIDE_STATUS_BAR] ?: defaults.hideStatusBar,
            focusActive = this[Keys.FOCUS_ACTIVE] ?: defaults.focusActive,
            focusStartedAtEpochMs = this[Keys.FOCUS_STARTED_AT] ?: defaults.focusStartedAtEpochMs,
            focusDurationMinutes = this[Keys.FOCUS_DURATION_MINUTES] ?: defaults.focusDurationMinutes,
            favorites = this[Keys.FAVORITES].toFavorites(defaults.favorites),
            favoritesSeeded = this[Keys.FAVORITES_SEEDED] ?: defaults.favoritesSeeded,
            onboardingComplete = this[Keys.ONBOARDING_COMPLETE] ?: defaults.onboardingComplete,
        )
    }

    private fun LauncherSettings.writeInto(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[Keys.FRICTION_LEVEL] = frictionLevel.name
        prefs[Keys.ENVIRONMENT_MODE] = environmentMode.name
        prefs[Keys.DEFAULT_OPEN_DELAY_SECONDS] = defaultOpenDelaySeconds
        prefs[Keys.DISPLAY_MODE] = displayMode.name
        prefs[Keys.SHOW_RECENTS] = showRecents
        prefs[Keys.SHOW_SUGGESTIONS] = showSuggestions
        prefs[Keys.OPENING_DELAYS_ENABLED] = openingDelaysEnabled
        prefs[Keys.INTENT_PROMPT_ENABLED] = intentPromptEnabled
        prefs[Keys.SLOW_MODE_ENABLED] = slowModeEnabled
        prefs[Keys.ANALOG_MODE_ENABLED] = analogModeEnabled
        prefs[Keys.HIDE_SOCIAL_APPS] = hideSocialApps
        prefs[Keys.ONE_APP_AT_A_TIME] = oneAppAtATime
        prefs[Keys.DOPAMINE_DETECTION_ENABLED] = dopamineDetectionEnabled
        prefs[Keys.DYNAMIC_MINIMALISM_ENABLED] = dynamicMinimalismEnabled
        prefs[Keys.REWARD_REAL_LIFE_ENABLED] = rewardRealLifeEnabled
        prefs[Keys.RECOVERY_MODE_ENABLED] = recoveryModeEnabled
        prefs[Keys.DEAD_END_FEEDS_ENABLED] = deadEndFeedsEnabled
        prefs[Keys.EINK_SIMULATION_ENABLED] = einkSimulationEnabled
        prefs[Keys.GRAYSCALE_ENABLED] = grayscaleEnabled
        prefs[Keys.THEME_PREFERENCE] = themePreference.name
        prefs[Keys.LIMIT_EXTENSIONS_PER_DAY] = limitExtensionsPerDay
        prefs[Keys.LIMIT_EXTRA_MINUTES_PER_DAY] = limitExtraMinutesPerDay
        prefs[Keys.LIMIT_MINUTES_PER_EXTENSION] = limitMinutesPerExtension
        prefs[Keys.PENDING_LIMIT_EXTENSIONS_PER_DAY] = pendingLimitExtensionsPerDay
        prefs[Keys.PENDING_LIMIT_EXTRA_MINUTES_PER_DAY] = pendingLimitExtraMinutesPerDay
        prefs[Keys.LIMIT_CAPS_PENDING_SINCE] = limitCapsPendingSinceDayStartEpochMs
        prefs[Keys.COLLECT_USAGE_ANALYTICS] = collectUsageAnalyticsEnabled
        prefs[Keys.ANALYTICS_RETENTION_DAYS] = analyticsRetentionDays
        prefs[Keys.KIOSK_MODE_ENABLED] = kioskModeEnabled
        prefs[Keys.HIDE_STATUS_BAR] = hideStatusBar
        prefs[Keys.FOCUS_ACTIVE] = focusActive
        prefs[Keys.FOCUS_STARTED_AT] = focusStartedAtEpochMs
        prefs[Keys.FOCUS_DURATION_MINUTES] = focusDurationMinutes
        prefs[Keys.FAVORITES] = favorites.joinToString(FAVORITES_DELIMITER)
        prefs[Keys.FAVORITES_SEEDED] = favoritesSeeded
        prefs[Keys.ONBOARDING_COMPLETE] = onboardingComplete
    }

    // -- Enum / list parsing helpers ---------------------------------------------

    /**
     * "MONK" is the old name for [FrictionLevel.MEDIUM]. Anyone who picked that tier before
     * the rename has it written to disk, so map it forward rather than silently dropping
     * them back to the LIGHT default.
     */
    private fun String?.toFrictionLevel(default: FrictionLevel): FrictionLevel = when (this) {
        null -> default
        LEGACY_MONK -> FrictionLevel.MEDIUM
        else -> FrictionLevel.entries.firstOrNull { it.name == this } ?: default
    }

    private fun String?.toEnvironmentMode(default: EnvironmentMode): EnvironmentMode =
        this?.let { name -> EnvironmentMode.entries.firstOrNull { it.name == name } } ?: default

    private fun String?.toDisplayMode(default: AppDisplayMode): AppDisplayMode =
        this?.let { name -> AppDisplayMode.entries.firstOrNull { it.name == name } } ?: default

    private fun String?.toThemePreference(default: ThemePreference): ThemePreference =
        this?.let { name -> ThemePreference.entries.firstOrNull { it.name == name } } ?: default

    private fun String?.toFavorites(default: List<String>): List<String> = when (this) {
        null -> default
        "" -> emptyList()
        else -> split(FAVORITES_DELIMITER).filter { it.isNotEmpty() }
    }

    private object Keys {
        val FRICTION_LEVEL = stringPreferencesKey("friction_level")
        val ENVIRONMENT_MODE = stringPreferencesKey("environment_mode")
        val DEFAULT_OPEN_DELAY_SECONDS = intPreferencesKey("default_open_delay_seconds")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val SHOW_RECENTS = booleanPreferencesKey("show_recents")
        val SHOW_SUGGESTIONS = booleanPreferencesKey("show_suggestions")
        val OPENING_DELAYS_ENABLED = booleanPreferencesKey("opening_delays_enabled")
        val INTENT_PROMPT_ENABLED = booleanPreferencesKey("intent_prompt_enabled")
        val SLOW_MODE_ENABLED = booleanPreferencesKey("slow_mode_enabled")
        val ANALOG_MODE_ENABLED = booleanPreferencesKey("analog_mode_enabled")
        val HIDE_SOCIAL_APPS = booleanPreferencesKey("hide_social_apps")
        val ONE_APP_AT_A_TIME = booleanPreferencesKey("one_app_at_a_time")
        val DOPAMINE_DETECTION_ENABLED = booleanPreferencesKey("dopamine_detection_enabled")
        val DYNAMIC_MINIMALISM_ENABLED = booleanPreferencesKey("dynamic_minimalism_enabled")
        val REWARD_REAL_LIFE_ENABLED = booleanPreferencesKey("reward_real_life_enabled")
        val RECOVERY_MODE_ENABLED = booleanPreferencesKey("recovery_mode_enabled")
        val DEAD_END_FEEDS_ENABLED = booleanPreferencesKey("dead_end_feeds_enabled")
        val EINK_SIMULATION_ENABLED = booleanPreferencesKey("eink_simulation_enabled")
        val GRAYSCALE_ENABLED = booleanPreferencesKey("grayscale_enabled")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val LIMIT_EXTENSIONS_PER_DAY = intPreferencesKey("limit_extensions_per_day")
        val LIMIT_EXTRA_MINUTES_PER_DAY = intPreferencesKey("limit_extra_minutes_per_day")
        val LIMIT_MINUTES_PER_EXTENSION = intPreferencesKey("limit_minutes_per_extension")
        val PENDING_LIMIT_EXTENSIONS_PER_DAY = intPreferencesKey("pending_limit_extensions_per_day")
        val PENDING_LIMIT_EXTRA_MINUTES_PER_DAY =
            intPreferencesKey("pending_limit_extra_minutes_per_day")
        val LIMIT_CAPS_PENDING_SINCE = longPreferencesKey("limit_caps_pending_since")
        val COLLECT_USAGE_ANALYTICS = booleanPreferencesKey("collect_usage_analytics")
        val ANALYTICS_RETENTION_DAYS = intPreferencesKey("analytics_retention_days")
        val KIOSK_MODE_ENABLED = booleanPreferencesKey("kiosk_mode_enabled")
        val HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
        val FOCUS_ACTIVE = booleanPreferencesKey("focus_active")
        val FOCUS_STARTED_AT = longPreferencesKey("focus_started_at")
        val FOCUS_DURATION_MINUTES = intPreferencesKey("focus_duration_minutes")
        val FAVORITES = stringPreferencesKey("favorites")
        val FAVORITES_SEEDED = booleanPreferencesKey("favorites_seeded")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    private object LegacyKeys {
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")

        /** Breath Unlock was removed; drop the orphaned key. */
        val BREATH_UNLOCK_ENABLED = booleanPreferencesKey("breath_unlock_enabled")
    }

    private companion object {
        const val FAVORITES_DELIMITER = "\n"

        /** Pre-rename name of [FrictionLevel.MEDIUM]. */
        const val LEGACY_MONK = "MONK"
    }
}
