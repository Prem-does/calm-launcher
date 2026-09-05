package com.calmlauncher.data.system

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits the current wall-clock time in epoch millis: once immediately, then once per second
 * aligned to the second boundary. This keeps active focus progress fluid without accumulating
 * timer drift. Collect on a background dispatcher.
 */
@Singleton
class ClockTicker @Inject constructor() {

    val time: Flow<Long> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            // Sleep until the start of the next second.
            val msIntoSecond = now % SECOND_MS
            val untilNextSecond = SECOND_MS - msIntoSecond
            delay(untilNextSecond)
        }
    }

    private companion object {
        const val SECOND_MS = 1_000L
    }
}
