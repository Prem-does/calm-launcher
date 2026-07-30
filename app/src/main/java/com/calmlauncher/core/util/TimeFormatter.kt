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

/**
 * A reminder's due time, phrased relative to today: "9:30 AM", "Yesterday, 9:30 AM", or
 * "Fri 3 Oct, 9:30 AM" for anything further out.
 *
 * The relative wording matters on the reminder overlay, where the question the user is silently
 * asking is "is this now, or did I miss it?" — an absolute date alone makes them do that
 * arithmetic themselves.
 */
fun formatReminderDueTime(
    dueEpochMs: Long,
    nowEpochMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    val due = Instant.ofEpochMilli(dueEpochMs).atZone(zone)
    val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
    val clock = due.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

    val daysApart = due.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay()
    return when (daysApart) {
        0L -> clock
        -1L -> "Yesterday, $clock"
        1L -> "Tomorrow, $clock"
        else -> {
            val date = due.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
            "$date, $clock"
        }
    }
}
