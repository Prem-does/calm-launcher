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
            breathUnlockEnabled = this[Keys.BREATH_UNLOCK_ENABLED] ?: defaults.breathUnlockEnabled,
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
            pinEnabled = this[Keys.PIN_ENABLED] ?: defaults.pinEnabled,
            pinHash = this[Keys.PIN_HASH] ?: defaults.pinHash,
            kioskModeEnabled = this[Keys.KIOSK_MODE_ENABLED] ?: defaults.kioskModeEnabled,
            hideStatusBar = this[Keys.HIDE_STATUS_BAR] ?: defaults.hideStatusBar,
            focusActive = this[Keys.FOCUS_ACTIVE] ?: defaults.focusActive,
            focusStartedAtEpochMs = this[Keys.FOCUS_STARTED_AT] ?: defaults.focusStartedAtEpochMs,
            focusDurationMinutes = this[Keys.FOCUS_DURATION_MINUTES] ?: defaults.focusDurationMinutes,
            favorites = this[Keys.FAVORITES].toFavorites(defaults.favorites),
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
        prefs[Keys.BREATH_UNLOCK_ENABLED] = breathUnlockEnabled
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
        prefs[Keys.PIN_ENABLED] = pinEnabled
        if (pinHash != null) prefs[Keys.PIN_HASH] = pinHash else prefs.remove(Keys.PIN_HASH)
        prefs[Keys.KIOSK_MODE_ENABLED] = kioskModeEnabled
        prefs[Keys.HIDE_STATUS_BAR] = hideStatusBar
        prefs[Keys.FOCUS_ACTIVE] = focusActive
        prefs[Keys.FOCUS_STARTED_AT] = focusStartedAtEpochMs
        prefs[Keys.FOCUS_DURATION_MINUTES] = focusDurationMinutes
        prefs[Keys.FAVORITES] = favorites.joinToString(FAVORITES_DELIMITER)
        prefs[Keys.ONBOARDING_COMPLETE] = onboardingComplete
    }

    // -- Enum / list parsing helpers ---------------------------------------------

    private fun String?.toFrictionLevel(default: FrictionLevel): FrictionLevel =
        this?.let { name -> FrictionLevel.entries.firstOrNull { it.name == name } } ?: default

    private fun String?.toEnvironmentMode(default: EnvironmentMode): EnvironmentMode =
        this?.let { name -> EnvironmentMode.entries.firstOrNull { it.name == name } } ?: default

    private fun String?.toDisplayMode(default: AppDisplayMode): AppDisplayMode =
        this?.let { name -> AppDisplayMode.entries.firstOrNull { it.name == name } } ?: default

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
        val BREATH_UNLOCK_ENABLED = booleanPreferencesKey("breath_unlock_enabled")
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
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val KIOSK_MODE_ENABLED = booleanPreferencesKey("kiosk_mode_enabled")
        val HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
        val FOCUS_ACTIVE = booleanPreferencesKey("focus_active")
        val FOCUS_STARTED_AT = longPreferencesKey("focus_started_at")
        val FOCUS_DURATION_MINUTES = intPreferencesKey("focus_duration_minutes")
        val FAVORITES = stringPreferencesKey("favorites")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    private companion object {
        const val FAVORITES_DELIMITER = "\n"
    }
}
