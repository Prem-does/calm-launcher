package com.calmlauncher.data.repository

import com.calmlauncher.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
	fun observeSettings(): Flow<SettingsEntity?>
	suspend fun saveSettings(settings: SettingsEntity)
}

