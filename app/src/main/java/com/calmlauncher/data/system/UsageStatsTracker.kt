package com.calmlauncher.data.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.calmlauncher.domain.model.ScreenTimeRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads foreground usage from [UsageStatsManager]. Requires the user to have granted the
 * Usage Access special permission (PACKAGE_USAGE_STATS); without it every query returns an
 * empty record rather than throwing. Day boundaries are local midnight.
 */
@Singleton
class UsageStatsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /** True if this app currently holds the Usage Access permission. */
    fun hasPermission(): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** Aggregated foreground usage for today (local midnight → now). */
    suspend fun todayForeground(): ScreenTimeRecord {
        val dayStart = startOfDay(System.currentTimeMillis())
        return aggregate(dayStart, System.currentTimeMillis(), dayStart)
    }

    /**
     * One [ScreenTimeRecord] per local day in [[startMs], [endMs]] (inclusive of the day
     * containing each bound). Days with no usage still appear as empty records so callers
     * get a contiguous series.
     */
    suspend fun rangeForeground(startMs: Long, endMs: Long): List<ScreenTimeRecord> {
        if (!hasPermission() || usageStatsManager == null) {
            return emptyDays(startMs, endMs)
        }
        val result = ArrayList<ScreenTimeRecord>()
        var dayStart = startOfDay(startMs)
        val lastDayStart = startOfDay(endMs)
        while (dayStart <= lastDayStart) {
            val dayEnd = (dayStart + DAY_MS).coerceAtMost(System.currentTimeMillis())
            result += aggregate(dayStart, dayEnd, dayStart)
            dayStart += DAY_MS
        }
        return result
    }

    // -- internals ---------------------------------------------------------------

    private fun aggregate(
        rangeStart: Long,
        rangeEnd: Long,
        dayStartKey: Long,
    ): ScreenTimeRecord {
        val manager = usageStatsManager
        if (!hasPermission() || manager == null || rangeEnd <= rangeStart) {
            return ScreenTimeRecord.empty(dayStartKey)
        }
        val stats = runCatching {
            manager.queryAndAggregateUsageStats(rangeStart, rangeEnd)
        }.getOrNull() ?: return ScreenTimeRecord.empty(dayStartKey)

        val perApp = HashMap<String, Long>()
        var total = 0L
        for ((pkg, usage) in stats) {
            if (pkg == context.packageName) continue
            val fg = usage.totalTimeInForeground
            if (fg <= 0L) continue
            perApp[pkg] = (perApp[pkg] ?: 0L) + fg
            total += fg
        }
        return ScreenTimeRecord(
            dayStartEpochMs = dayStartKey,
            totalForegroundMs = total,
            perApp = perApp,
        )
    }

    private fun emptyDays(startMs: Long, endMs: Long): List<ScreenTimeRecord> {
        val result = ArrayList<ScreenTimeRecord>()
        var dayStart = startOfDay(startMs)
        val lastDayStart = startOfDay(endMs)
        while (dayStart <= lastDayStart) {
            result += ScreenTimeRecord.empty(dayStart)
            dayStart += DAY_MS
        }
        return result
    }

    private fun startOfDay(epochMs: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
