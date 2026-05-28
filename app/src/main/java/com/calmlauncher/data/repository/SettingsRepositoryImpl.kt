package com.calmlauncher.data.repository

import com.calmlauncher.data.db.SettingsDao
import com.calmlauncher.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(private val settingsDao: SettingsDao) : SettingsRepository {
    override fun observeSettings(): Flow<SettingsEntity?> = settingsDao.observeSettings()
    override suspend fun saveSettings(settings: SettingsEntity) = settingsDao.upsert(settings)
}
