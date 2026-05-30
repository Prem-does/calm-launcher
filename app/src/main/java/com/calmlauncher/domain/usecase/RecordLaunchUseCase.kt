package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.policy.RiskEvaluator
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.RiskRepository
import com.calmlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Records a launch event, then re-runs the Dopamine Detection Engine over the last hour of
 * activity (including the just-recorded event) and persists the updated [com.calmlauncher.domain.model.RiskState].
 *
 * Risk recomputation is skipped (but the event is still logged) when the user has turned
 * Dopamine Detection off, leaving the previous risk state untouched.
 */
class RecordLaunchUseCase @Inject constructor(
    private val launchEventRepository: LaunchEventRepository,
    private val riskRepository: RiskRepository,
    private val riskEvaluator: RiskEvaluator,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(event: LaunchEvent) {
        launchEventRepository.record(event)

        if (!settingsRepository.current().dopamineDetectionEnabled) return

        val now = event.timestampEpochMs
        val recentEvents = launchEventRepository.since(now - RECENT_WINDOW_MS)
        val previous = riskRepository.current()
        val updated = riskEvaluator.evaluate(
            recentEvents = recentEvents,
            previous = previous,
            nowEpochMs = now,
        )
        riskRepository.set(updated)
    }

    private companion object {
        const val RECENT_WINDOW_MS = 60L * 60L * 1000L // 60 minutes
    }
}
