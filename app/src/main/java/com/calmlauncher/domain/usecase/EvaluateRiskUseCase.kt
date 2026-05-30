package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.policy.RiskEvaluator
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.RiskRepository
import javax.inject.Inject

/**
 * Recomputes and persists the [com.calmlauncher.domain.model.RiskState] from the last hour of
 * launch events. Intended to be driven by a periodic background worker so that risk decays
 * (e.g. CALM after a quiet hour) even when the user isn't actively opening apps.
 */
class EvaluateRiskUseCase @Inject constructor(
    private val launchEventRepository: LaunchEventRepository,
    private val riskRepository: RiskRepository,
    private val riskEvaluator: RiskEvaluator,
) {
    suspend operator fun invoke(nowEpochMs: Long = System.currentTimeMillis()) {
        val recentEvents = launchEventRepository.since(nowEpochMs - RECENT_WINDOW_MS)
        val previous = riskRepository.current()
        val updated = riskEvaluator.evaluate(
            recentEvents = recentEvents,
            previous = previous,
            nowEpochMs = nowEpochMs,
        )
        riskRepository.set(updated)
    }

    private companion object {
        const val RECENT_WINDOW_MS = 60L * 60L * 1000L // 60 minutes
    }
}
