package com.calmlauncher.data.repository

import com.calmlauncher.data.db.ScreenTimeDao
import com.calmlauncher.data.db.toDomain
import com.calmlauncher.data.db.toEntity
import com.calmlauncher.data.system.UsageStatsTracker
import com.calmlauncher.di.IoDispatcher
import com.calmlauncher.domain.model.ScreenTimeRecord
import com.calmlauncher.domain.repository.ScreenTimeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
.\gradlew.bat assembleDebugimport kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

/**
 * Daily foreground usage. The persisted [ScreenTimeEntity] rows are the observable source
 * of truth; [refresh] pulls fresh numbers from [UsageStatsTracker] and upserts them so the
 * Flows update. When usage access is missing, refresh writes an empty record for today.
 */
class ScreenTimeRepositoryImpl @Inject constructor(
    private val usageStatsTracker: UsageStatsTracker,
    private val screenTimeDao: ScreenTimeDao,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ScreenTimeRepository {

    override fun observeToday(): Flow<ScreenTimeRecord> {
        val dayStart = startOfToday()
        return screenTimeDao.observe(dayStart).map { it?.toDomain() ?: ScreenTimeRecord.empty(dayStart) }
    }

    override suspend fun today(): ScreenTimeRecord = withContext(dispatcher) {
        val dayStart = startOfToday()
        screenTimeDao.get(dayStart)?.toDomain() ?: ScreenTimeRecord.empty(dayStart)
    }

    override fun observeRange(startEpochMs: Long, endEpochMs: Long): Flow<List<ScreenTimeRecord>> =
        screenTimeDao.observeRange(startEpochMs, endEpochMs)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh() = withContext(dispatcher) {
        val record = usageStatsTracker.todayForeground()
        screenTimeDao.upsert(record.toEntity())
    }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
