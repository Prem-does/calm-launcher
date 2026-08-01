package com.calmlauncher.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.calmlauncher.domain.model.AccentColor
import com.calmlauncher.domain.model.AppearanceSettings
import com.calmlauncher.domain.model.ClockStyle
import com.calmlauncher.domain.model.FontScale
import com.calmlauncher.domain.model.FontStyle
import com.calmlauncher.domain.model.HomeGridColumns
import com.calmlauncher.domain.model.LayoutDensity
import com.calmlauncher.domain.model.SearchBarStyle
import com.calmlauncher.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes [AppearanceSettings].
 *
 * Shares the same Preferences store as [SettingsDataStore] but keeps its keys under an `appearance_`
 * prefix and never touches a behavioural key. Every value is stored by [Enum.name] and parsed
 * defensively, so an unrecognised value — from a downgrade, or a hand-edited file — falls back to
 * the documented default rather than throwing on startup.
 */
@Singleton
class AppearanceDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val appearance: Flow<AppearanceSettings> = dataStore.data.map { it.toAppearance() }

    suspend fun current(): AppearanceSettings = appearance.first()

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        dataStore.edit { prefs ->
            transform(prefs.toAppearance()).writeInto(prefs)
        }
    }

    private fun Preferences.toAppearance(): AppearanceSettings {
        val defaults = AppearanceSettings()
        return AppearanceSettings(
            themeMode = enumOr(this[Keys.THEME_MODE], defaults.themeMode),
            accent = enumOr(this[Keys.ACCENT], defaults.accent),
            fontStyle = enumOr(this[Keys.FONT_STYLE], defaults.fontStyle),
            fontScale = enumOr(this[Keys.FONT_SCALE], defaults.fontScale),
            gridColumns = enumOr(this[Keys.GRID_COLUMNS], defaults.gridColumns),
            clockStyle = enumOr(this[Keys.CLOCK_STYLE], defaults.clockStyle),
            searchBarStyle = enumOr(this[Keys.SEARCH_BAR_STYLE], defaults.searchBarStyle),
            density = enumOr(this[Keys.DENSITY], defaults.density),
        )
    }

    private fun AppearanceSettings.writeInto(
        prefs: androidx.datastore.preferences.core.MutablePreferences,
    ) {
        prefs[Keys.THEME_MODE] = themeMode.name
        prefs[Keys.ACCENT] = accent.name
        prefs[Keys.FONT_STYLE] = fontStyle.name
        prefs[Keys.FONT_SCALE] = fontScale.name
        prefs[Keys.GRID_COLUMNS] = gridColumns.name
        prefs[Keys.CLOCK_STYLE] = clockStyle.name
        prefs[Keys.SEARCH_BAR_STYLE] = searchBarStyle.name
        prefs[Keys.DENSITY] = density.name
    }

    /** Parse an enum by name, falling back to [default] for anything unrecognised. */
    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default

    private object Keys {
        val THEME_MODE = stringPreferencesKey("appearance_theme_mode")
        val ACCENT = stringPreferencesKey("appearance_accent")
        val FONT_STYLE = stringPreferencesKey("appearance_font_style")
        val FONT_SCALE = stringPreferencesKey("appearance_font_scale")
        val GRID_COLUMNS = stringPreferencesKey("appearance_grid_columns")
        val CLOCK_STYLE = stringPreferencesKey("appearance_clock_style")
        val SEARCH_BAR_STYLE = stringPreferencesKey("appearance_search_bar_style")
        val DENSITY = stringPreferencesKey("appearance_density")
    }
}
