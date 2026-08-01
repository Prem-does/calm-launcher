package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.AppearanceSettings
import kotlinx.coroutines.flow.Flow

/**
 * Purely visual preferences.
 *
 * Kept separate from [SettingsRepository] on purpose. That one is read by the ModeEngine, the
 * friction rules and the blocking services; this one is read only by the theme and by screens
 * deciding how to draw themselves. Anything that reaches for this repository to make a *decision*
 * is a bug — see [AppearanceSettings].
 */
interface AppearanceRepository {
    val appearance: Flow<AppearanceSettings>

    suspend fun current(): AppearanceSettings

    suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings)

    /** Restore every appearance option to its default, leaving behaviour untouched. */
    suspend fun reset()
}
