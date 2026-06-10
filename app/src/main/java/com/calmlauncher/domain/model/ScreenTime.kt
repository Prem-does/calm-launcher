package com.calmlauncher.domain.model

/**
 * Foreground usage for a single day. [perApp] maps package name -> foreground millis.
 */
data class ScreenTimeRecord(
    val dayStartEpochMs: Long,
    val totalForegroundMs: Long,
    val perApp: Map<String, Long> = emptyMap(),
) {
    val totalMinutes: Long get() = totalForegroundMs / 60_000L

    /** Human-friendly "1h 12m" style string for the Settings/Home surfaces. */
    fun format(): String {
        val minutes = totalMinutes
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            minutes > 0 -> "${m}m"
            else -> "<1m"
        }
    }

    companion object {
        fun empty(dayStartEpochMs: Long) = ScreenTimeRecord(dayStartEpochMs, 0L)
    }
}
