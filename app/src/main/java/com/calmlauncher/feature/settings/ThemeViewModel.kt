package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.ThemePreference
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exposes and updates the persisted launcher appearance preference. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = settingsRepository.settings
        .map { it.themePreference }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemePreference.DARK,
        )

    fun setThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(themePreference = themePreference) }
        }
    }
}