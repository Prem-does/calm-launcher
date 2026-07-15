package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.Insight
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.ScreenTimeRecord
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.ScreenTimeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * The **Calm AI Assistant**: builds a short list of neutral, non-judgemental [Insight]s from
 * the week's launch events and daily screen time. The tone is deliberately observational
 * ("You opened X 12 times yesterday") rather than scolding — the app reflects, it doesn't nag.
 *
 * All phrasing is derived inside the [combine] mapping from the two repository flows. A one-off
 * "now" is captured at subscription time only to bound the screen-time range and to phrase
 * relative days ("yesterday"); this is a use-case boundary concern, not pure policy.
 */
class BuildInsightsUseCase @Inject constructor(
    private val appLimitRepository: AppLimitRepository,
    private val launchEventRepository: LaunchEventRepository,
    private val screenTimeRepository: ScreenTimeRepository,
) {
    operator fun invoke(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<Insight>> {
        val rangeStart = nowEpochMs - WEEK_MS
        return combine(
            launchEventRepository.observeWeek(),
            screenTimeRepository.observeRange(rangeStart, nowEpochMs),
            appLimitRepository.observeTodayEvents(),
        ) { events, screenTime, limitEvents ->
            buildInsights(events, screenTime, limitEvents, nowEpochMs, zoneId)
        }
    }

    /** Pure assembly of the insight list from raw inputs. */
    private fun buildInsights(
        events: List<LaunchEvent>,
        screenTime: List<ScreenTimeRecord>,
        limitEvents: List<AppLimitEvent>,
        nowEpochMs: Long,
        zoneId: ZoneId,
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        mostOpenedYesterdayInsight(events, nowEpochMs, zoneId)?.let(insights::add)
        peakHourInsight(events, zoneId)?.let(insights::add)
        screenTimeTrendInsight(screenTime)?.let(insights::add)
        appLimitInsight(limitEvents)?.let(insights::add)

        return insights
    }

    /** "You opened Instagram 12 times yesterday." */
    private fun mostOpenedYesterdayInsight(
        events: List<LaunchEvent>,
        nowEpochMs: Long,
        zoneId: ZoneId,
    ): Insight? {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)
        val counts = HashMap<String, Int>()
        for (e in events) {
            val date = Instant.ofEpochMilli(e.timestampEpochMs).atZone(zoneId).toLocalDate()
            if (date == yesterday) counts[e.packageName] = (counts[e.packageName] ?: 0) + 1
        }
        val top = counts.maxByOrNull { it.value } ?: return null
        if (top.value < MIN_OPENS_TO_MENTION) return null
        return Insight("You opened ${friendlyName(top.key)} ${top.value} times yesterday.")
    }

    /** "Most opens happen around 9pm." */
    private fun peakHourInsight(events: List<LaunchEvent>, zoneId: ZoneId): Insight? {
        if (events.size < MIN_EVENTS_FOR_PATTERN) return null
        val perHour = IntArray(24)
        for (e in events) {
            val hour = Instant.ofEpochMilli(e.timestampEpochMs).atZone(zoneId).hour
            perHour[hour]++
        }
        var peak = 0
        for (h in 1 until 24) if (perHour[h] > perHour[peak]) peak = h
        if (perHour[peak] == 0) return null
        return Insight("Most opens happen around ${formatHour(peak)}.")
    }

    /**
     * "Screen time is down 20% this week." Compares the most recent half of the range with the
     * earlier half. Only reported when the change is meaningful (≥ [MIN_TREND_PERCENT]).
     */
    private fun screenTimeTrendInsight(screenTime: List<ScreenTimeRecord>): Insight? {
        if (screenTime.size < 4) return null
        val sorted = screenTime.sortedBy { it.dayStartEpochMs }
        val mid = sorted.size / 2
        val earlier = sorted.subList(0, mid)
        val recent = sorted.subList(mid, sorted.size)
        val earlierAvg = earlier.map { it.totalForegroundMs }.average()
        val recentAvg = recent.map { it.totalForegroundMs }.average()
        if (earlierAvg <= 0.0) return null

        val deltaPercent = ((recentAvg - earlierAvg) / earlierAvg * 100.0).roundToInt()
        if (kotlin.math.abs(deltaPercent) < MIN_TREND_PERCENT) return null

        return if (deltaPercent < 0) {
            Insight("Screen time is down ${-deltaPercent}% this week.")
        } else {
            Insight("Screen time is up $deltaPercent% this week.")
        }
    }

    /** "App Limits blocked 3 launches today." */
    private fun appLimitInsight(limitEvents: List<AppLimitEvent>): Insight? {
        val blocked = limitEvents.filter { it.eventType == AppLimitEventType.BLOCKED }
        if (blocked.isEmpty()) return null
        val top = blocked.groupingBy { it.label }.eachCount().maxByOrNull { it.value }
        return if (top != null && top.value > 1) {
            Insight("App Limits blocked ${blocked.size} launches today. ${top.key} hit the limit ${top.value} times.")
        } else {
            Insight("App Limits blocked ${blocked.size} launches today.")
        }
    }

    /** Best-effort display name from a package id: "com.instagram.android" -> "Instagram". */
    private fun friendlyName(packageName: String): String {
        val candidate = packageName
            .substringBefore("/")
            .split('.')
            .lastOrNull { it.isNotBlank() && it != "android" && it != "app" }
            ?: packageName
        return candidate.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /** "21" -> "9pm", "0" -> "12am", "13" -> "1pm". */
    private fun formatHour(hour24: Int): String {
        val suffix = if (hour24 < 12) "am" else "pm"
        val h12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        return "$h12$suffix"
    }

    private companion object {
        const val WEEK_MS = 7L * 24L * 60L * 60L * 1000L
        const val MIN_OPENS_TO_MENTION = 3
        const val MIN_EVENTS_FOR_PATTERN = 5
        const val MIN_TREND_PERCENT = 10
    }
}
