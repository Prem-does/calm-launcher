package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AppearanceSettings
import com.calmlauncher.domain.model.ThemeMode
import com.calmlauncher.domain.model.ThemePreference
import com.calmlauncher.domain.repository.AppearanceRepository
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The launcher's appearance, for the composition root and for the older light/dark switch on the
 * Settings screen.
 *
 * [AppearanceRepository] is the single source of truth for how the launcher looks. This ViewModel
 * exposes it to the theme, and keeps that older [ThemePreference] control working now that
 * Customization owns the theme: [setThemePreference] writes through to a [ThemeMode] and
 * [themePreference] reads back from one, so the two surfaces cannot disagree about which theme is
 * active. Leaving the old switch pointed at a field the theme no longer reads would have quietly
 * turned it into a dead control.
 *
 * `LauncherSettings.themePreference` is still written for backwards compatibility — anything reading
 * that field keeps seeing a correct value — but nothing renders from it any more.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** The full appearance, driving [com.calmlauncher.core.designsystem.theme.CalmTheme]. */
    val appearance: StateFlow<AppearanceSettings> = appearanceRepository.appearance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppearanceSettings(),
        )

    /**
     * Light/dark as the older two-state switch understands it. [ThemeMode.SYSTEM] has no
     * representation there, so it reports dark — the launcher's own default, and the side the switch
     * should rest on when the device decides.
     */
    val themePreference: StateFlow<ThemePreference> = appearanceRepository.appearance
        .map { appearance ->
            when (appearance.themeMode) {
                ThemeMode.LIGHT -> ThemePreference.LIGHT
                ThemeMode.DARK -> ThemePreference.DARK
                ThemeMode.SYSTEM -> ThemePreference.DARK
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreference.DARK,
        )

    fun setThemePreference(themePreference: ThemePreference) {
        val mode = when (themePreference) {
            ThemePreference.LIGHT -> ThemeMode.LIGHT
            ThemePreference.DARK -> ThemeMode.DARK
        }
        viewModelScope.launch {
            appearanceRepository.update { it.copy(themeMode = mode) }
            // Mirrored so any older reader of LauncherSettings.themePreference stays correct.
            settingsRepository.update { it.copy(themePreference = themePreference) }
        }
    }
}
