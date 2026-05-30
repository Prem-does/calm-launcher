package com.calmlauncher.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.UiRestrictionState
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.usecase.ObserveRestrictionStateUseCase
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
    observeRestriction: ObserveRestrictionStateUseCase,
) : ViewModel() {
    val onboardingComplete: StateFlow<Boolean?> =
        settingsRepository.settings
            .map { it.onboardingComplete }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val restriction: StateFlow<UiRestrictionState> = observeRestriction()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiRestrictionState())
}
