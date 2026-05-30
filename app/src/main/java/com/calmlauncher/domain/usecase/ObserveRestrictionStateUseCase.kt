package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.UiRestrictionState
import com.calmlauncher.domain.policy.ModeEngine
import com.calmlauncher.domain.repository.RiskRepository
import com.calmlauncher.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Streams the observable [UiRestrictionState] by combining the live settings and risk flows
 * through the pure [ModeEngine.restrictionState]. Screens collect this to enforce grayscale,
 * minimalism, motion and which surfaces (suggestions/recents) are visible.
 */
class ObserveRestrictionStateUseCase @Inject constructor(
    private val modeEngine: ModeEngine,
    private val settingsRepository: SettingsRepository,
    private val riskRepository: RiskRepository,
) {
    operator fun invoke(): Flow<UiRestrictionState> =
        combine(settingsRepository.settings, riskRepository.state) { settings, risk ->
            modeEngine.restrictionState(settings, risk)
        }.distinctUntilChanged()
}
