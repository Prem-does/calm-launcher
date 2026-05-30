package com.calmlauncher.data.repository

import com.calmlauncher.data.datastore.SettingsDataStore
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** [SettingsRepository] backed by the Preferences [SettingsDataStore]. */
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : SettingsRepository {

    override val settings: Flow<LauncherSettings> = settingsDataStore.settings

    override suspend fun current(): LauncherSettings = settingsDataStore.current()

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        settingsDataStore.update(transform)
    }
}
