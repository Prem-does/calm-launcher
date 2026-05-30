package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Starts and stops a **Real Focus Session**. Persisting [com.calmlauncher.domain.model.LauncherSettings.focusActive]
 * (plus start time and duration) is all the domain needs: the [com.calmlauncher.domain.policy.ModeEngine]
 * reads these flags to hard-block quick-exit apps for the duration of the session.
 */
class ToggleFocusUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /** Begin a focus session of [durationMinutes], anchored at [nowEpochMs]. */
    suspend fun start(durationMinutes: Int, nowEpochMs: Long = System.currentTimeMillis()) {
        val safeDuration = durationMinutes.coerceAtLeast(1)
        settingsRepository.update {
            it.copy(
                focusActive = true,
                focusStartedAtEpochMs = nowEpochMs,
                focusDurationMinutes = safeDuration,
            )
        }
    }

    /** End the current focus session, leaving the chosen duration as the next default. */
    suspend fun stop() {
        settingsRepository.update {
            it.copy(
                focusActive = false,
                focusStartedAtEpochMs = 0L,
            )
        }
    }
}
