package com.calmlauncher.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimeFormatter {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    fun time(now: LocalDateTime): String = now.format(timeFormatter)
    fun date(now: LocalDateTime): String = now.format(dateFormatter)
}
