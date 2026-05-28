package com.calmlauncher.data.repository

import com.calmlauncher.data.db.ScreenTimeDao
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import kotlinx.coroutines.flow.Flow

class ScreenTimeRepositoryImpl(private val screenTimeDao: ScreenTimeDao) : ScreenTimeRepository {
    override fun observeDay(dayKey: String): Flow<ScreenTimeEntity?> = screenTimeDao.observeDay(dayKey)
    override suspend fun setDayTotal(dayKey: String, minutes: Int) = screenTimeDao.upsert(ScreenTimeEntity(dayKey = dayKey, minutesUsed = minutes))
    override suspend fun getDay(dayKey: String): ScreenTimeEntity? = screenTimeDao.getDay(dayKey)
}
