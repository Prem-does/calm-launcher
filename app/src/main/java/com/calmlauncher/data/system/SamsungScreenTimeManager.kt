package com.calmlauncher.data.system

import android.content.Context
import android.os.Build
import com.calmlauncher.domain.model.SamsungScreenTimeFormatter
import com.calmlauncher.domain.model.SamsungScreenTimeSummary
import com.calmlauncher.domain.model.ScreenTimeRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Samsung-compatible screen-time facade over the existing UsageStats-based implementation.
 *
 * The public API mirrors the copy-paste prompts, but the data still comes from the same
 * local UsageStats reconstruction used everywhere else in the app.
 */
@Singleton
class SamsungScreenTimeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsTracker: UsageStatsTracker,
) {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun isSamsungDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("samsung") || brand.contains("samsung")
    }

    fun isSamsungDigitalWellbeingAvailable(): Boolean =
        isSamsungDevice() && listOf(
            "com.samsung.android.lool",
            "com.samsung.android.wellbeing",
            "com.samsung.android.smartsuggestions",
            "com.samsung.android.settings",
        ).any { isInstalled(it) }

    fun isSamsungFamilyLinkAvailable(): Boolean = listOf(
        "com.google.android.apps.kids.familylink",
        "com.google.android.apps.kids.familylinkhelper",
        "com.samsung.android.familylink",
    ).any { isInstalled(it) }

    suspend fun getSamsungScreenTimeForApp(packageName: String, date: LocalDate): Long =
        recordFor(date).perApp[packageName] ?: 0L

    suspend fun getAllSamsungScreenTime(date: LocalDate): Map<String, Long> =
        recordFor(date).perApp

    suspend fun getSamsungTotalScreenTime(date: LocalDate): Long =
        recordFor(date).totalForegroundMs

    suspend fun getSamsungDashboardSummary(date: LocalDate): SamsungScreenTimeSummary {
        val record = recordFor(date)
        return SamsungScreenTimeSummary(
            date = date,
            totalScreenTimeMs = record.totalForegroundMs,
            perAppScreenTimeMs = record.perApp,
        )
    }

    fun formatSamsungScreenTime(milliseconds: Long): String =
        SamsungScreenTimeFormatter.format(milliseconds)

    /** Public Samsung cloud sync APIs are not exposed here, so this remains a no-op. */
    suspend fun syncWithSamsungCloud(): Boolean = false

    private suspend fun recordFor(date: LocalDate): ScreenTimeRecord {
        val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return usageStatsTracker.rangeForeground(startOfDay, endOfDay).firstOrNull()
            ?: ScreenTimeRecord.empty(startOfDay)
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)
}