package com.calmlauncher.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Decides the start destination: null while settings are loading, then true/false for
 * whether onboarding has been completed. Kept tiny so the root can resolve quickly.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val onboardingComplete: StateFlow<Boolean?> =
        settingsRepository.settings
            .map { it.onboardingComplete }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
