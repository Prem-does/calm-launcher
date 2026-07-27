package com.calmlauncher.data.system

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.calmlauncher.domain.model.ScreenTimeRecord
import com.calmlauncher.domain.model.UsageSessionRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * How far before a range to start reading events, so a session already running at the range
 * start is seen from its real beginning rather than invented from the boundary.
 */
private const val LOOKBACK_MS = 24L * 60L * 60L * 1000L

/** Below this, an interval is an activity-handover artefact rather than usage. */
private const val MIN_INTERVAL_MS = 1_000L

/**
 * [UsageEvents.Event] type ids, spelled out rather than referenced.
 *
 * The system emits these regardless of the API level the app compiles against, and the ids
 * are frozen. Naming them here keeps the replay readable without needing SDK_INT guards for
 * constants that were only *published* later (ACTIVITY_STOPPED in 29, the screen/keyguard
 * events in 28, DEVICE_SHUTDOWN in 31) but were always being emitted.
 */
private object EventType {
    /** Also MOVE_TO_FOREGROUND on pre-Q devices; the id is the same. */
    const val ACTIVITY_RESUMED = 1

    /** Also MOVE_TO_BACKGROUND on pre-Q devices; the id is the same. */
    const val ACTIVITY_PAUSED = 2
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_SHOWN = 17
    const val ACTIVITY_STOPPED = 23
    const val DEVICE_SHUTDOWN = 26
}

/**
 * Reconstructs foreground usage from [UsageStatsManager] events. Requires the user to have
 * granted the Usage Access special permission (PACKAGE_USAGE_STATS); without it every query
 * returns an empty record rather than throwing. Day boundaries are local midnight.
 *
 * The event stream is replayed into a list of foreground intervals per package. Three things
 * matter for the numbers to be trustworthy:
 *
 *  - **Activities are counted, not packages.** An app switching between its own activities
 *    emits `RESUMED(B)` before `PAUSED(A)`; tracking which activities are up means the app
 *    stays "one session" across the handover instead of opening a second overlapping one.
 *  - **The screen going off ends everything.** `PAUSED` is not guaranteed when a device
 *    sleeps, locks, or shuts down. Without closing on those events, an app the user left
 *    open before bed accrues eight hours of "usage" overnight — the single largest source
 *    of wrong numbers in a usage tracker.
 *  - **Total screen time merges overlapping intervals** rather than summing per-app time,
 *    so two apps briefly overlapping at a handover can't push a day past 24 hours.
 *
 * Queries reach [LOOKBACK_MS] before the range so a session that started earlier is seen
 * whole and then clamped, rather than being invented from the range boundary.
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
        return aggregate(dayStart, System.currentTimeMillis())
    }

    /** Foreground usage for a single package today, in milliseconds. */
    suspend fun todayForegroundFor(packageName: String): Long =
        todayForeground().perApp[packageName] ?: 0L

    /** Individual foreground sessions for today, reconstructed from usage events. */
    suspend fun todaySessions(): List<UsageSessionRecord> {
        val dayStart = startOfDay(System.currentTimeMillis())
        return sessionsForRange(dayStart, System.currentTimeMillis())
    }

    /** Sessions for whole local days in the requested range, tagged with the day they fall in. */
    suspend fun rangeSessions(startMs: Long, endMs: Long): List<UsageSessionRecord> =
        rangeUsage(startMs, endMs).flatMap { it.sessions }

    /**
     * One [ScreenTimeRecord] per local day in [[startMs], [endMs]] (inclusive of the day
     * containing each bound). Days with no usage still appear as empty records so callers
     * get a contiguous series.
     */
    suspend fun rangeForeground(startMs: Long, endMs: Long): List<ScreenTimeRecord> =
        rangeUsage(startMs, endMs).map { it.record }

    /**
     * Both views of a day — the aggregate and the individual sessions — from a single replay
     * of that day's events. Callers rebuilding stored history want both, and reading the
     * event stream twice per day is the expensive half of a refresh.
     */
    suspend fun rangeUsage(startMs: Long, endMs: Long): List<DayUsage> {
        if (!hasPermission() || usageStatsManager == null) {
            return emptyDays(startMs, endMs).map { DayUsage(it, emptyList()) }
        }
        val result = ArrayList<DayUsage>()
        forEachDay(startMs, endMs) { dayStart, dayEnd ->
            val intervals = foregroundIntervals(dayStart, dayEnd)
            result += DayUsage(
                record = aggregate(dayStart, dayEnd, intervals),
                sessions = intervals.toSessionRecords(dayStart),
            )
        }
        return result
    }

    /** One local day's usage: the daily total plus the sessions it was made of. */
    data class DayUsage(
        val record: ScreenTimeRecord,
        val sessions: List<UsageSessionRecord>,
    )

    // -- Event replay --------------------------------------------------------------

    /**
     * Every foreground interval that overlaps [[rangeStart], [rangeEnd]], clamped to those
     * bounds. Intervals shorter than [MIN_INTERVAL_MS] after clamping are dropped: they are
     * transient handovers, not time the user actually spent anywhere.
     */
    private fun foregroundIntervals(rangeStart: Long, rangeEnd: Long): List<Interval> {
        val manager = usageStatsManager
        if (!hasPermission() || manager == null || rangeEnd <= rangeStart) return emptyList()

        val queryStart = (rangeStart - LOOKBACK_MS).coerceAtLeast(0L)
        val events = runCatching { manager.queryEvents(queryStart, rangeEnd) }.getOrNull()
            ?: return emptyList()

        val intervals = ArrayList<Interval>()
        // Per package: which of its activities are currently resumed, and when the package
        // as a whole came to the foreground.
        val resumedActivities = HashMap<String, MutableSet<String>>()
        val foregroundSince = HashMap<String, Long>()
        val event = UsageEvents.Event()

        fun closeAll(timestamp: Long) {
            foregroundSince.forEach { (pkg, since) ->
                intervals += Interval(pkg, since, timestamp)
            }
            foregroundSince.clear()
            resumedActivities.clear()
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val timestamp = event.timeStamp

            // The screen turning off, the keyguard appearing or the device shutting down all
            // end foreground time whether or not an ACTIVITY_PAUSED follows.
            if (event.eventType == EventType.SCREEN_NON_INTERACTIVE ||
                event.eventType == EventType.KEYGUARD_SHOWN ||
                event.eventType == EventType.DEVICE_SHUTDOWN
            ) {
                closeAll(timestamp)
                continue
            }

            val packageName = event.packageName ?: continue
            // The launcher's own time is not screen time the user is trying to reduce.
            if (packageName == context.packageName) continue
            val activity = event.className ?: packageName

            when (event.eventType) {
                EventType.ACTIVITY_RESUMED -> {
                    val activities = resumedActivities.getOrPut(packageName) { HashSet() }
                    if (activities.isEmpty()) {
                        foregroundSince[packageName] = timestamp
                    }
                    activities += activity
                }

                // PAUSED and STOPPED both mean "this activity is no longer up". Treating
                // them as the same removal keeps the count right when only one arrives.
                EventType.ACTIVITY_PAUSED,
                EventType.ACTIVITY_STOPPED,
                -> {
                    val activities = resumedActivities[packageName] ?: continue
                    if (!activities.remove(activity)) continue
                    if (activities.isEmpty()) {
                        resumedActivities.remove(packageName)
                        // An unmatched close (the resume happened before the lookback window)
                        // is dropped rather than invented from the range boundary.
                        foregroundSince.remove(packageName)?.let { since ->
                            intervals += Interval(packageName, since, timestamp)
                        }
                    }
                }
            }
        }

        // Anything still in the foreground when the window ends runs up to the end.
        closeAll(rangeEnd)

        return intervals.mapNotNull { it.clampTo(rangeStart, rangeEnd) }
    }

    private fun aggregate(rangeStart: Long, rangeEnd: Long): ScreenTimeRecord =
        aggregate(rangeStart, rangeEnd, foregroundIntervals(rangeStart, rangeEnd))

    private fun aggregate(
        rangeStart: Long,
        rangeEnd: Long,
        intervals: List<Interval>,
    ): ScreenTimeRecord {
        if (rangeEnd <= rangeStart) return ScreenTimeRecord.empty(rangeStart)

        // Per app, merge that app's own intervals so overlapping activity handovers inside
        // one app can't be counted twice.
        val perApp = intervals
            .groupBy { it.packageName }
            .mapValues { (_, appIntervals) -> mergedDuration(appIntervals) }
            .filterValues { it > 0L }

        return ScreenTimeRecord(
            dayStartEpochMs = rangeStart,
            // Merged across every app: the time the screen was on with *something* up.
            totalForegroundMs = mergedDuration(intervals).coerceAtMost(rangeEnd - rangeStart),
            perApp = perApp,
        )
    }

    private fun sessionsForRange(rangeStart: Long, rangeEnd: Long): List<UsageSessionRecord> =
        foregroundIntervals(rangeStart, rangeEnd).toSessionRecords(startOfDay(rangeStart))

    private fun List<Interval>.toSessionRecords(dayStart: Long): List<UsageSessionRecord> =
        map { interval ->
            UsageSessionRecord(
                dayStartEpochMs = dayStart,
                packageName = interval.packageName,
                appName = friendlyName(interval.packageName),
                startTimeEpochMs = interval.start,
                endTimeEpochMs = interval.end,
                durationMinutes = (interval.durationMs / 60_000L).toInt(),
            )
        }

    // -- Helpers -------------------------------------------------------------------

    private data class Interval(
        val packageName: String,
        val start: Long,
        val end: Long,
    ) {
        val durationMs: Long get() = (end - start).coerceAtLeast(0L)

        /** This interval restricted to the window, or null if it doesn't meaningfully overlap. */
        fun clampTo(rangeStart: Long, rangeEnd: Long): Interval? {
            val clampedStart = start.coerceIn(rangeStart, rangeEnd)
            val clampedEnd = end.coerceIn(rangeStart, rangeEnd)
            if (clampedEnd - clampedStart < MIN_INTERVAL_MS) return null
            return Interval(packageName, clampedStart, clampedEnd)
        }
    }

    /** Total time covered by [intervals], counting overlapping stretches once. */
    private fun mergedDuration(intervals: List<Interval>): Long {
        if (intervals.isEmpty()) return 0L
        val sorted = intervals.sortedBy { it.start }

        var total = 0L
        var currentStart = sorted.first().start
        var currentEnd = sorted.first().end

        for (i in 1 until sorted.size) {
            val interval = sorted[i]
            if (interval.start <= currentEnd) {
                currentEnd = maxOf(currentEnd, interval.end)
            } else {
                total += currentEnd - currentStart
                currentStart = interval.start
                currentEnd = interval.end
            }
        }
        return total + (currentEnd - currentStart)
    }

    /** Last-resort display name for a package the caller couldn't label. */
    private fun friendlyName(packageName: String): String {
        val candidate = packageName
            .substringBefore("/")
            .split('.')
            .lastOrNull { it.isNotBlank() && it != "android" && it != "app" }
            ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Walk whole local days from the day containing [startMs] to the day containing [endMs].
     * Uses [Calendar] rather than fixed 24h arithmetic so DST transitions don't shift the
     * boundaries by an hour and smear usage into the wrong day.
     */
    private inline fun forEachDay(startMs: Long, endMs: Long, action: (Long, Long) -> Unit) {
        val now = System.currentTimeMillis()
        val cursor = Calendar.getInstance().apply { timeInMillis = startOfDay(startMs) }
        val lastDayStart = startOfDay(endMs)
        while (cursor.timeInMillis <= lastDayStart) {
            val dayStart = cursor.timeInMillis
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = cursor.timeInMillis.coerceAtMost(now)
            if (dayEnd > dayStart) action(dayStart, dayEnd)
        }
    }

    private fun emptyDays(startMs: Long, endMs: Long): List<ScreenTimeRecord> {
        val result = ArrayList<ScreenTimeRecord>()
        forEachDay(startMs, endMs) { dayStart, _ -> result += ScreenTimeRecord.empty(dayStart) }
        return result
    }

    private fun startOfDay(epochMs: Long): Long = Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

}
