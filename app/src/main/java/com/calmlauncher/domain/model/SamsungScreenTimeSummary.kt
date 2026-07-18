package com.calmlauncher.domain.model

import java.time.LocalDate

/**
 * Samsung-flavored screen-time snapshot backed by the app's existing UsageStats pipeline.
 */
data class SamsungScreenTimeSummary(
    val date: LocalDate,
    val totalScreenTimeMs: Long,
    val perAppScreenTimeMs: Map<String, Long>,
) {
    val topApps: List<Pair<String, Long>> = perAppScreenTimeMs.entries
        .asSequence()
        .filter { it.value > 0L }
        .sortedByDescending { it.value }
        .map { it.key to it.value }
        .toList()

    val appCount: Int = topApps.size

    fun getFormattedTotalTime(): String = SamsungScreenTimeFormatter.format(totalScreenTimeMs)
}

object SamsungScreenTimeFormatter {
    fun format(milliseconds: Long): String {
        if (milliseconds <= 0L) return "0m"

        val totalMinutes = milliseconds / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            minutes > 0L -> "${minutes}m"
            else -> "<1m"
        }
    }
}