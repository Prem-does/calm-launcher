package com.calmlauncher.data.repository

import com.calmlauncher.data.db.LaunchEventDao
import com.calmlauncher.data.db.entity.LaunchEventEntity
import kotlinx.coroutines.flow.Flow

class LaunchEventRepositoryImpl(private val launchEventDao: LaunchEventDao) : LaunchEventRepository {
    override fun observeRecentLaunches(limit: Int): Flow<List<LaunchEventEntity>> = launchEventDao.observeRecentLaunches(limit)

    override suspend fun recordLaunchReason(packageName: String, label: String, reason: String) {
        launchEventDao.insert(
            LaunchEventEntity(
                packageName = packageName,
                label = label,
                reason = reason.trim(),
                timestampMillis = System.currentTimeMillis()
            )
        )
    }
}