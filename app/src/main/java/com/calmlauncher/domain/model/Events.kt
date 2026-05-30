package com.calmlauncher.domain.model

/** A recorded app open, including the user's stated intent if captured. */
data class LaunchEvent(
    val id: Long = 0L,
    val packageName: String,
    val category: AppCategory,
    val timestampEpochMs: Long,
    val reason: String? = null,
    val source: LaunchSource = LaunchSource.APP_LIST,
)

/** A nightly reflection prompt and the user's optional written response. */
data class ReflectionEntry(
    val id: Long = 0L,
    val dayStartEpochMs: Long,
    val prompt: String,
    val response: String? = null,
    val createdAtEpochMs: Long,
)

/** A neutral, non-judgemental piece of insight text (Calm AI Assistant). */
data class Insight(val text: String)

/** Snapshot of an active (or inactive) focus session. */
data class FocusSession(
    val active: Boolean = false,
    val startedAtEpochMs: Long = 0L,
    val durationMinutes: Int = 0,
    val quote: String = "",
) {
    val endsAtEpochMs: Long get() = startedAtEpochMs + durationMinutes * 60_000L
    fun remainingMs(now: Long): Long = (endsAtEpochMs - now).coerceAtLeast(0L)
}
