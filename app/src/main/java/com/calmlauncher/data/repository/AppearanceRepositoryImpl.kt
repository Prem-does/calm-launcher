package com.calmlauncher.data.repository

import com.calmlauncher.data.datastore.AppearanceDataStore
import com.calmlauncher.domain.model.AppearanceSettings
import com.calmlauncher.domain.repository.AppearanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** [AppearanceRepository] backed by the Preferences [AppearanceDataStore]. */
class AppearanceRepositoryImpl @Inject constructor(
    private val appearanceDataStore: AppearanceDataStore,
) : AppearanceRepository {

    override val appearance: Flow<AppearanceSettings> = appearanceDataStore.appearance

    override suspend fun current(): AppearanceSettings = appearanceDataStore.current()

    override suspend fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        appearanceDataStore.update(transform)
    }

    override suspend fun reset() {
        appearanceDataStore.update { AppearanceSettings() }
    }
}
