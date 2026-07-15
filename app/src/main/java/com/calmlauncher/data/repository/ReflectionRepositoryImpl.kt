package com.calmlauncher.data.repository

import com.calmlauncher.data.db.ReflectionDao
import com.calmlauncher.data.db.toDomain
import com.calmlauncher.data.db.toEntity
import com.calmlauncher.domain.model.ReflectionEntry
import com.calmlauncher.domain.repository.ReflectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Nightly reflections backed by Room. */
class ReflectionRepositoryImpl @Inject constructor(
    private val reflectionDao: ReflectionDao,
) : ReflectionRepository {

    override fun observeRecent(limit: Int): Flow<List<ReflectionEntry>> =
        reflectionDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun latestFor(dayStartEpochMs: Long): ReflectionEntry? =
        reflectionDao.latestFor(dayStartEpochMs)?.toDomain()

    override suspend fun insert(entry: ReflectionEntry) {
        reflectionDao.insert(entry.toEntity())
    }
}
