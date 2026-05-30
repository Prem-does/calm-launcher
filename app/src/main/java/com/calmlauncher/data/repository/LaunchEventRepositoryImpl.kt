package com.calmlauncher.data.repository

import com.calmlauncher.data.db.LaunchEventDao
import com.calmlauncher.data.db.toDomain
import com.calmlauncher.data.db.toEntity
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.repository.LaunchEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Append-only launch log backed by Room. */
class LaunchEventRepositoryImpl @Inject constructor(
    private val launchEventDao: LaunchEventDao,
) : LaunchEventRepository {

    override suspend fun record(event: LaunchEvent) {
        launchEventDao.insert(event.toEntity())
    }

    override fun observeSince(sinceEpochMs: Long): Flow<List<LaunchEvent>> =
        launchEventDao.observeSince(sinceEpochMs).map { rows -> rows.map { it.toDomain() } }

    override suspend fun since(sinceEpochMs: Long): List<LaunchEvent> =
        launchEventDao.since(sinceEpochMs).map { it.toDomain() }

    override fun observeWeek(): Flow<List<LaunchEvent>> {
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        return observeSince(weekAgo)
    }
}
