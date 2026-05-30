package com.calmlauncher.domain.policy

import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.RiskState
import com.calmlauncher.domain.model.RiskTier
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * The **Dopamine Detection Engine**.
 *
 * Looks at roughly the last hour of [LaunchEvent]s and produces a [RiskState] describing how
 * compulsive the recent usage pattern looks. Higher risk tightens friction and minimalism
 * elsewhere in the app, while a calm pattern relaxes the interface (Reward Real Life).
 *
 * The function is pure and deterministic: the only notion of "now" is [nowEpochMs], and the
 * only locale dependency is the [ZoneId] used to decide what counts as "late night" (passed
 * in, defaulting to the system zone at the call boundary).
 *
 * ### Signals (measured within the [WINDOW_MS] window ending at `nowEpochMs`)
 *  - **repeatedOpens** — "excess" opens of the same app. The first [FREE_OPENS_PER_APP] opens
 *    of any package are free; everything beyond that for a package is counted, summed across
 *    all apps. Captures "checking the same app over and over".
 *  - **rapidSwitches** — number of launches that open a *different* app than the previous one
 *    within [RAPID_SWITCH_MS]. Captures restless app-hopping.
 *  - **lateNightLaunches** — events whose local hour falls in [LATE_NIGHT_START_HOUR, 24) ∪
 *    [0, LATE_NIGHT_END_HOUR). Captures doom-scrolling past bedtime.
 *  - **longestSessionMs** — the longest run of events where each consecutive gap is below
 *    [SESSION_GAP_MS], measured first-to-last. A rough proxy for an unbroken phone session
 *    (naturally capped by the window length).
 *
 * ### Score (0..100)
 * `score = repeatedOpens·8 + rapidSwitches·6 + lateNightLaunches·7 + sessionPoints`, clamped
 * to 0..100, where `sessionPoints` grows with session length up to [MAX_SESSION_POINTS].
 *
 * ### Tier thresholds
 *  - score `< 25`           → CALM
 *  - score `< 50`           → ELEVATED
 *  - score `< 80`           → HIGH
 *  - score `>= 80`          → RECOVERY
 *  - **sustained HIGH**: if the [previous] tier was already HIGH (or RECOVERY) and the current
 *    score is still in the HIGH band (`>= 50`), we escalate to RECOVERY rather than letting a
 *    persistently bad pattern sit at HIGH indefinitely.
 */
class RiskEvaluator @Inject constructor() {

    fun evaluate(
        recentEvents: List<LaunchEvent>,
        previous: RiskState,
        nowEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): RiskState {
        val windowStart = nowEpochMs - WINDOW_MS
        // Keep only in-window events, oldest → newest, so gap/switch logic is well defined.
        val events = recentEvents
            .filter { it.timestampEpochMs in windowStart..nowEpochMs }
            .sortedBy { it.timestampEpochMs }

        if (events.isEmpty()) {
            return RiskState(
                tier = RiskTier.CALM,
                score = 0,
                repeatedOpens = 0,
                rapidSwitches = 0,
                lateNightLaunches = 0,
                longestSessionMs = 0L,
                updatedAtEpochMs = nowEpochMs,
            )
        }

        val repeatedOpens = computeRepeatedOpens(events)
        val rapidSwitches = computeRapidSwitches(events)
        val lateNightLaunches = computeLateNightLaunches(events, zoneId)
        val longestSessionMs = computeLongestSessionMs(events)

        val score = computeScore(repeatedOpens, rapidSwitches, lateNightLaunches, longestSessionMs)
        val tier = computeTier(score, previous.tier)

        return RiskState(
            tier = tier,
            score = score,
            repeatedOpens = repeatedOpens,
            rapidSwitches = rapidSwitches,
            lateNightLaunches = lateNightLaunches,
            longestSessionMs = longestSessionMs,
            updatedAtEpochMs = nowEpochMs,
        )
    }

    /** Sum over apps of `max(0, opens - FREE_OPENS_PER_APP)`. */
    private fun computeRepeatedOpens(events: List<LaunchEvent>): Int {
        val perApp = HashMap<String, Int>()
        for (e in events) perApp[e.packageName] = (perApp[e.packageName] ?: 0) + 1
        var excess = 0
        for (count in perApp.values) {
            if (count > FREE_OPENS_PER_APP) excess += count - FREE_OPENS_PER_APP
        }
        return excess
    }

    /** Consecutive launches that switch to a different app within [RAPID_SWITCH_MS]. */
    private fun computeRapidSwitches(events: List<LaunchEvent>): Int {
        var switches = 0
        for (i in 1 until events.size) {
            val prev = events[i - 1]
            val curr = events[i]
            val gap = curr.timestampEpochMs - prev.timestampEpochMs
            if (curr.packageName != prev.packageName && gap in 0..RAPID_SWITCH_MS) {
                switches++
            }
        }
        return switches
    }

    /** Events whose local hour is between 23:00 and 05:00. */
    private fun computeLateNightLaunches(events: List<LaunchEvent>, zoneId: ZoneId): Int {
        var count = 0
        for (e in events) {
            val hour = Instant.ofEpochMilli(e.timestampEpochMs).atZone(zoneId).hour
            if (hour >= LATE_NIGHT_START_HOUR || hour < LATE_NIGHT_END_HOUR) count++
        }
        return count
    }

    /**
     * Longest first→last span of a run of events where each consecutive gap is below
     * [SESSION_GAP_MS]. Events assumed sorted ascending by timestamp.
     */
    private fun computeLongestSessionMs(events: List<LaunchEvent>): Long {
        if (events.size < 2) return 0L
        var longest = 0L
        var runStart = events.first().timestampEpochMs
        for (i in 1 until events.size) {
            val gap = events[i].timestampEpochMs - events[i - 1].timestampEpochMs
            if (gap > SESSION_GAP_MS) {
                // Run broke; close it out and start a new one at this event.
                longest = maxOf(longest, events[i - 1].timestampEpochMs - runStart)
                runStart = events[i].timestampEpochMs
            }
        }
        longest = maxOf(longest, events.last().timestampEpochMs - runStart)
        return longest
    }

    private fun computeScore(
        repeatedOpens: Int,
        rapidSwitches: Int,
        lateNightLaunches: Int,
        longestSessionMs: Long,
    ): Int {
        val sessionMinutes = longestSessionMs / 60_000L
        // Roughly 1 point per 2 minutes of unbroken session, capped.
        val sessionPoints = (sessionMinutes / 2L)
            .coerceAtMost(MAX_SESSION_POINTS.toLong())
            .toInt()

        val raw = repeatedOpens * REPEATED_OPEN_WEIGHT +
            rapidSwitches * RAPID_SWITCH_WEIGHT +
            lateNightLaunches * LATE_NIGHT_WEIGHT +
            sessionPoints
        return raw.coerceIn(0, 100)
    }

    private fun computeTier(score: Int, previousTier: RiskTier): RiskTier {
        val base = when {
            score < CALM_MAX -> RiskTier.CALM
            score < ELEVATED_MAX -> RiskTier.ELEVATED
            score < HIGH_MAX -> RiskTier.HIGH
            else -> RiskTier.RECOVERY
        }
        // Sustained HIGH: a persistently bad pattern graduates from HIGH to RECOVERY.
        val sustainedHigh =
            (previousTier == RiskTier.HIGH || previousTier == RiskTier.RECOVERY) &&
                score >= ELEVATED_MAX
        return if (sustainedHigh) RiskTier.RECOVERY else base
    }

    private companion object {
        // ----- Windows -----
        const val WINDOW_MS = 60L * 60L * 1000L      // 60 minutes
        const val RAPID_SWITCH_MS = 30L * 1000L      // < 30s between distinct apps
        const val SESSION_GAP_MS = 30L * 60L * 1000L // gap that ends a "session"

        // ----- Signal tuning -----
        const val FREE_OPENS_PER_APP = 2             // first 2 opens of any app are "free"
        const val LATE_NIGHT_START_HOUR = 23
        const val LATE_NIGHT_END_HOUR = 5

        // ----- Score weights -----
        const val REPEATED_OPEN_WEIGHT = 8
        const val RAPID_SWITCH_WEIGHT = 6
        const val LATE_NIGHT_WEIGHT = 7
        const val MAX_SESSION_POINTS = 15

        // ----- Tier thresholds -----
        const val CALM_MAX = 25
        const val ELEVATED_MAX = 50
        const val HIGH_MAX = 80
    }
}
