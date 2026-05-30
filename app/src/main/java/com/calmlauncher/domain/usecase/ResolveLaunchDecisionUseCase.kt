package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.AppLaunchRequest
import com.calmlauncher.domain.model.LaunchDecision
import com.calmlauncher.domain.policy.ModeEngine
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.RiskRepository
import com.calmlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Resolves an [AppLaunchRequest] into a [LaunchDecision] by gathering the current settings,
 * persisted risk state and the last 60 minutes of launch events, then delegating to the
 * pure [ModeEngine]. The launch gate UI consumes the resulting ordered friction steps.
 */
class ResolveLaunchDecisionUseCase @Inject constructor(
    private val modeEngine: ModeEngine,
    private val settingsRepository: SettingsRepository,
    private val riskRepository: RiskRepository,
    private val launchEventRepository: LaunchEventRepository,
) {
    suspend operator fun invoke(
        request: AppLaunchRequest,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): LaunchDecision {
        val settings = settingsRepository.current()
        val risk = riskRepository.current()
        val recentEvents = launchEventRepository.since(nowEpochMs - RECENT_WINDOW_MS)
        return modeEngine.resolve(
            request = request,
            settings = settings,
            risk = risk,
            recentEvents = recentEvents,
            nowEpochMs = nowEpochMs,
        )
    }

    private companion object {
        const val RECENT_WINDOW_MS = 60L * 60L * 1000L // 60 minutes
    }
}
