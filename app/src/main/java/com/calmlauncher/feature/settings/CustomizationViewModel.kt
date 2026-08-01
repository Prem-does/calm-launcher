package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AppearanceSettings
import com.calmlauncher.domain.model.ClockStyle
import com.calmlauncher.domain.model.FontScale
import com.calmlauncher.domain.model.FontStyle
import com.calmlauncher.domain.model.HomeGridColumns
import com.calmlauncher.domain.model.LayoutDensity
import com.calmlauncher.domain.model.SearchBarStyle
import com.calmlauncher.domain.model.ThemeMode
import com.calmlauncher.domain.repository.AppearanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs Settings → Customization.
 *
 * It injects [AppearanceRepository] and nothing else — no [com.calmlauncher.domain.repository.SettingsRepository],
 * no limit or reminder repository. That is enforced by the constructor rather than by convention:
 * this screen structurally *cannot* change launcher behaviour, because it has no access to anything
 * that decides behaviour.
 *
 * Every setter writes straight through to the repository, whose flow feeds the theme at the
 * composition root, so a change is visible on the next frame with no restart.
 */
@HiltViewModel
class CustomizationViewModel @Inject constructor(
    private val appearanceRepository: AppearanceRepository,
) : ViewModel() {

    val appearance: StateFlow<AppearanceSettings> = appearanceRepository.appearance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppearanceSettings(),
        )

    fun setThemeMode(value: ThemeMode) = update { it.copy(themeMode = value) }

    fun setFontStyle(value: FontStyle) = update { it.copy(fontStyle = value) }

    fun setFontScale(value: FontScale) = update { it.copy(fontScale = value) }

    fun setGridColumns(value: HomeGridColumns) = update { it.copy(gridColumns = value) }

    fun setClockStyle(value: ClockStyle) = update { it.copy(clockStyle = value) }

    fun setSearchBarStyle(value: SearchBarStyle) = update { it.copy(searchBarStyle = value) }

    fun setDensity(value: LayoutDensity) = update { it.copy(density = value) }

    /** Back to the default look. Touches appearance only — no behavioural setting is reset. */
    fun resetToDefaults() {
        viewModelScope.launch { appearanceRepository.reset() }
    }

    private fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        viewModelScope.launch { appearanceRepository.update(transform) }
    }
}
