package com.calmlauncher.data.repository

import com.calmlauncher.data.db.entity.ScreenTimeEntity
import kotlinx.coroutines.flow.Flow

interface ScreenTimeRepository {
	fun observeDay(dayKey: String): Flow<ScreenTimeEntity?>
	suspend fun setDayTotal(dayKey: String, minutes: Int)
	suspend fun getDay(dayKey: String): ScreenTimeEntity?
}

