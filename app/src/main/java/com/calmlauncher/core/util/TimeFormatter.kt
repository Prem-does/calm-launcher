package com.calmlauncher.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pure formatting helpers for the home clock and date. No leading zero on the hour in
 * 12-hour mode, matching the Stitch home design ("8:30").
 */
object TimeFormatter {

    private val timeFormatter12 = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
    private val timeFormatter24 = DateTimeFormatter.ofPattern("H:mm", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

    fun formatTime(epochMs: Long, use24h: Boolean, zone: ZoneId = ZoneId.systemDefault()): String {
        val dt = Instant.ofEpochMilli(epochMs).atZone(zone)
        return dt.format(if (use24h) timeFormatter24 else timeFormatter12)
    }

    fun formatDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val dt = Instant.ofEpochMilli(epochMs).atZone(zone)
        return dt.format(dateFormatter)
    }

    /** "Friday" — used for weekly-pattern insight phrasing. */
    fun dayOfWeek(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMs).atZone(zone).dayOfWeek
            .getDisplayName(TextStyle.FULL, Locale.getDefault())
}
