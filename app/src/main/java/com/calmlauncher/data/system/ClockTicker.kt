package com.calmlauncher.data.system

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits the current wall-clock time in epoch millis: once immediately, then once per
 * minute aligned to the minute boundary (so the displayed clock flips exactly when the
 * minute changes rather than drifting). Collect on a background dispatcher.
 */
@Singleton
class ClockTicker @Inject constructor() {

    val time: Flow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            // Sleep until the start of the next minute.
            val msIntoMinute = now % MINUTE_MS
            val untilNextMinute = MINUTE_MS - msIntoMinute
            delay(untilNextMinute)
        }
    }

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}
