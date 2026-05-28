package com.calmlauncher.data.repository

import com.calmlauncher.data.db.entity.LaunchEventEntity
import kotlinx.coroutines.flow.Flow

interface LaunchEventRepository {
    fun observeRecentLaunches(limit: Int = 50): Flow<List<LaunchEventEntity>>
    suspend fun recordLaunchReason(packageName: String, label: String, reason: String)
}