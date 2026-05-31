package com.calmlauncher.data.system

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.calmlauncher.domain.model.UsageSessionRecord
import com.calmlauncher.domain.model.ScreenTimeRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reconstructs foreground usage from [UsageStatsManager] events. Requires the user to have
 * granted the Usage Access special permission (PACKAGE_USAGE_STATS); without it every query
 * returns an empty record rather than throwing. Day boundaries are local midnight.
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

    /** Foreground usage for a single package today, in milliseconds. */
    suspend fun todayForegroundFor(packageName: String): Long =
        todayForeground().perApp[packageName] ?: 0L

    /** Individual foreground sessions for today, reconstructed from usage events. */
    suspend fun todaySessions(): List<UsageSessionRecord> {
        val dayStart = startOfDay(System.currentTimeMillis())
        return sessionsForRange(dayStart, System.currentTimeMillis())
    }

    /** Sessions for whole local days in the requested range. */
    suspend fun rangeSessions(startMs: Long, endMs: Long): List<UsageSessionRecord> {
        if (!hasPermission() || usageStatsManager == null) {
            return emptyList()
        }
        val result = ArrayList<UsageSessionRecord>()
        var dayStart = startOfDay(startMs)
        val lastDayStart = startOfDay(endMs)
        while (dayStart <= lastDayStart) {
            val dayEnd = (dayStart + DAY_MS).coerceAtMost(System.currentTimeMillis())
            result += sessionsForRange(dayStart, dayEnd)
            dayStart += DAY_MS
        }
        return result
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
        val perApp = HashMap<String, Long>()
        var total = 0L
        val activeSessions = HashMap<String, ActiveSession>()
        val events = runCatching { manager.queryEvents(rangeStart, rangeEnd) }.getOrNull()
            ?: return ScreenTimeRecord.empty(dayStartKey)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED,
                -> activeSessions.getOrPut(packageName) { ActiveSession() }.onEnter(event.timeStamp)

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                -> activeSessions[packageName]?.let { session ->
                    session.onExit(event.timeStamp)?.let { duration ->
                        perApp[packageName] = (perApp[packageName] ?: 0L) + duration
                        total += duration
                    }
                    if (session.isInactive) {
                        activeSessions.remove(packageName)
                    }
                }
            }
        }

        activeSessions.forEach { (packageName, session) ->
            session.closeAt(rangeEnd)?.let { duration ->
                perApp[packageName] = (perApp[packageName] ?: 0L) + duration
                total += duration
            }
        }

        return ScreenTimeRecord(
            dayStartEpochMs = dayStartKey,
            totalForegroundMs = total,
            perApp = perApp,
        )
    }

    private fun sessionsForRange(rangeStart: Long, rangeEnd: Long): List<UsageSessionRecord> {
        val manager = usageStatsManager
        if (!hasPermission() || manager == null || rangeEnd <= rangeStart) {
            return emptyList()
        }
        val events = runCatching { manager.queryEvents(rangeStart, rangeEnd) }.getOrNull()
            ?: return emptyList()
        val result = ArrayList<UsageSessionRecord>()
        val activeSessions = HashMap<String, SessionState>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (packageName == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED,
                -> activeSessions.getOrPut(packageName) { SessionState() }.onEnter(event.timeStamp)

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                -> activeSessions[packageName]?.let { session ->
                    session.onExit(event.timeStamp)?.let { record ->
                        result += UsageSessionRecord(
                            dayStartEpochMs = rangeStart,
                            packageName = packageName,
                            appName = friendlyName(packageName),
                            startTimeEpochMs = record.startedAt,
                            endTimeEpochMs = record.endedAt,
                            durationMinutes = (record.durationMs / 60_000L).toInt(),
                        )
                    }
                    if (session.isInactive) {
                        activeSessions.remove(packageName)
                    }
                }
            }
        }

        activeSessions.forEach { (packageName, session) ->
            session.closeAt(rangeEnd)?.let { record ->
                result += UsageSessionRecord(
                    dayStartEpochMs = rangeStart,
                    packageName = packageName,
                    appName = friendlyName(packageName),
                    startTimeEpochMs = record.startedAt,
                    endTimeEpochMs = record.endedAt,
                    durationMinutes = (record.durationMs / 60_000L).toInt(),
                )
            }
        }

        return result
    }

    private fun friendlyName(packageName: String): String {
        val candidate = packageName
            .substringBefore("/")
            .split('.')
            .lastOrNull { it.isNotBlank() && it != "android" && it != "app" }
            ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private data class SessionState(
        var activeCount: Int = 0,
        var startedAt: Long? = null,
    ) {
        val isInactive: Boolean get() = activeCount <= 0

        fun onEnter(timestamp: Long) {
            if (activeCount == 0) {
                startedAt = timestamp
            }
            activeCount += 1
        }

        fun onExit(timestamp: Long): SessionRecord? {
            if (activeCount <= 0) return null
            activeCount -= 1
            if (activeCount == 0) {
                val started = startedAt ?: return null
                startedAt = null
                return SessionRecord(started, timestamp, (timestamp - started).coerceAtLeast(0L))
            }
            return null
        }

        fun closeAt(timestamp: Long): SessionRecord? {
            val started = startedAt ?: return null
            startedAt = null
            activeCount = 0
            return SessionRecord(started, timestamp, (timestamp - started).coerceAtLeast(0L))
        }
    }

    private data class SessionRecord(
        val startedAt: Long,
        val endedAt: Long,
        val durationMs: Long,
    )

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

    private data class ActiveSession(
        var activeCount: Int = 0,
        var startedAt: Long? = null,
    ) {
        val isInactive: Boolean get() = activeCount <= 0

        fun onEnter(timestamp: Long) {
            if (activeCount == 0) {
                startedAt = timestamp
            }
            activeCount += 1
        }

        fun onExit(timestamp: Long): Long? {
            if (activeCount <= 0) return null
            activeCount -= 1
            if (activeCount == 0) {
                val started = startedAt ?: return null
                startedAt = null
                return (timestamp - started).coerceAtLeast(0L)
            }
            return null
        }

        fun closeAt(timestamp: Long): Long? {
            val started = startedAt ?: return null
            startedAt = null
            activeCount = 0
            return (timestamp - started).coerceAtLeast(0L)
        }
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
