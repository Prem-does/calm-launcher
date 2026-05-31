package com.calmlauncher.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AnalyticsCategory
import com.calmlauncher.domain.model.AnalyticsRange
import com.calmlauncher.domain.model.AppUsageRecord
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.NotificationEventType
import com.calmlauncher.domain.model.UsageSessionRecord
import com.calmlauncher.domain.model.UsageSortOrder

@Composable
fun ScreenTimeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val selectedDay = viewModel.selectedDay(state.snapshot)
    val topApps = viewModel.topApps(state.snapshot, state.sortOrder)
    val selectedSessions = viewModel.sessionsForDay(state.snapshot, selectedDay.dayStartEpochMs)
    val selectedUnlocks = viewModel.unlocksForDay(state.snapshot, selectedDay.dayStartEpochMs)
    val selectedNotifications = viewModel.notificationsForDay(state.snapshot, selectedDay.dayStartEpochMs)

    CalmScaffold(
        modifier = modifier,
        topBar = { CalmBackBar(title = "Usage Analytics", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel("Controls")
            AnalyticsControlRow(
                title = "Collect Usage Analytics",
                checked = state.collectionEnabled,
                onCheckedChange = viewModel::toggleCollection,
            )
            OptionRow(
                title = "Retention Period",
                value = "${state.retentionDays} days",
                onClick = { viewModel.setRetentionDays(nextRetention(state.retentionDays)) },
            )
            OptionRow(
                title = "Range",
                value = state.selectedRange.label(),
                onClick = { viewModel.selectRange(state.selectedRange.next()) },
            )
            OptionRow(
                title = "Sort Apps",
                value = state.sortOrder.label(),
                onClick = { viewModel.selectSortOrder(state.sortOrder.next()) },
            )
            OptionRow(
                title = "Export CSV",
                value = "Share",
                onClick = { shareAnalytics(context, state.snapshot, exportJson = false) },
            )
            OptionRow(
                title = "Export JSON",
                value = "Share",
                onClick = { shareAnalytics(context, state.snapshot, exportJson = true) },
            )
            OptionRow(
                title = "Clear Analytics History",
                value = "Delete all",
                onClick = viewModel::clearHistory,
            )

            SectionLabel("Today")
            SummaryBlock(
                title = "Overview",
                lines = listOf(
                    "Screen Time: ${formatMinutes(state.snapshot.today.totalScreenTimeMinutes)}",
                    "Unlocks: ${state.snapshot.today.unlockCount}",
                    "Notifications: ${state.snapshot.today.notificationCount}",
                    "Most Used App: ${topApps.firstOrNull()?.appName ?: "None"}",
                    "Longest Session: ${formatMinutes(state.snapshot.today.longestSessionMinutes)}",
                    "App Launches: ${state.snapshot.today.appLaunchCount}",
                ),
            )

            SummaryBlock(
                title = "Comparison",
                lines = listOf(
                    "Today: ${formatMinutes(state.snapshot.today.totalScreenTimeMinutes)}",
                    "Yesterday: ${formatMinutes(state.snapshot.yesterday.totalScreenTimeMinutes)}",
                    comparisonLine(state.snapshot.today.totalScreenTimeMinutes, state.snapshot.yesterday.totalScreenTimeMinutes, "screen time"),
                    comparisonLine(state.snapshot.today.unlockCount, state.snapshot.yesterday.unlockCount, "unlock count"),
                    comparisonLine(state.snapshot.today.notificationCount, state.snapshot.yesterday.notificationCount, "notifications"),
                ),
            )

            SectionLabel("Recent Days")
            state.snapshot.dailyHistory.takeLast(7).forEach { day ->
                HistoryRow(
                    day = day,
                    selected = day.dayStartEpochMs == selectedDay.dayStartEpochMs,
                    onClick = { viewModel.selectDay(day.dayStartEpochMs) },
                )
            }

            SummaryBlock(
                title = "Selected Day Detail",
                lines = listOf(
                    dayLabel(selectedDay.dayStartEpochMs),
                    "Screen Time: ${formatMinutes(selectedDay.totalScreenTimeMinutes)}",
                    "Unlocks: ${selectedDay.unlockCount}",
                    "Notifications: ${selectedDay.notificationCount}",
                    "Longest Session: ${formatMinutes(selectedDay.longestSessionMinutes)}",
                    "App Launches: ${selectedDay.appLaunchCount}",
                    "Sessions: ${selectedSessions.size}",
                ),
            )

            SectionLabel("Weekly Analytics")
            weeklyLabels().forEach { weekday ->
                val match = state.snapshot.dailyHistory.lastOrNull { dayName(it.dayStartEpochMs) == weekday }
                UsageRow(
                    label = weekday,
                    duration = match?.let { "${formatMinutes(it.totalScreenTimeMinutes)}  |  ${it.unlockCount} unlocks" } ?: "No data",
                )
            }

            SectionLabel("Monthly Analytics")
            SummaryBlock(
                title = "This Month vs Previous Month",
                lines = monthlySummaryLines(state.snapshot.dailyHistory),
            )

            SectionLabel("App Usage")
            topApps.forEach { app ->
                UsageRow(
                    label = app.appName,
                    duration = formatMinutes(app.usageMinutes),
                )
            }

            AppHeatmap(
                apps = topApps.take(5),
                history = state.snapshot.appUsage,
            )

            SectionLabel("Screen Time Heatmap")
            HeatmapRows(
                title = "Last ${state.snapshot.dailyHistory.takeLast(30).size} days",
                rows = state.snapshot.dailyHistory.takeLast(30).map { day ->
                    HeatmapRow(
                        label = dayLabel(day.dayStartEpochMs),
                        intensity = intensityChar(day.totalScreenTimeMinutes, state.snapshot.dailyHistory.map { it.totalScreenTimeMinutes }),
                    )
                },
            )

            SectionLabel("Unlock Analytics")
            SummaryBlock(
                title = "Unlock Totals",
                lines = listOf(
                    "Today: ${selectedUnlocks.size}",
                    "This Week: ${state.snapshot.unlocks.count { isWithinDays(it.dayStartEpochMs, 7) }}",
                    "This Month: ${state.snapshot.unlocks.count { isWithinDays(it.dayStartEpochMs, 30) }}",
                ),
            )
            unlockHourRows(selectedUnlocks).forEach { (label, value) ->
                UsageRow(label = label, duration = value.toString())
            }

            SectionLabel("Session Analytics")
            SummaryBlock(
                title = "Sessions",
                lines = sessionSummaryLines(selectedSessions, state.snapshot.sessions.filter { isWithinDays(it.dayStartEpochMs, 7) }),
            )
            state.snapshot.sessions.take(5).forEach { session ->
                UsageRow(
                    label = session.appName,
                    duration = formatMinutes(session.durationMinutes),
                )
            }

            SectionLabel("Notification Analytics")
            SummaryBlock(
                title = "Totals",
                lines = notificationSummaryLines(selectedNotifications),
            )
            notificationCountsByApp(selectedNotifications).forEach { (label, value) ->
                UsageRow(label = label, duration = value.toString())
            }

            SectionLabel("Social Media Analytics")
            categorySummary(topApps).forEach { line ->
                Text(
                    text = line,
                    style = CalmType.bodyLg,
                    color = CalmGray,
                    modifier = Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
                )
            }

            SectionLabel("Trend Detection")
            trendLines(state.snapshot.dailyHistory).forEach { line ->
                Text(
                    text = line,
                    style = CalmType.bodyLg,
                    color = CalmGray,
                    modifier = Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
                )
            }

            SectionLabel("Weekly Reflection")
            reflectionLines(state.snapshot.dailyHistory).forEach { line ->
                Text(
                    text = line,
                    style = CalmType.bodyLg,
                    color = CalmGray,
                    modifier = Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
                )
            }

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}

@Composable
private fun AnalyticsControlRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = CalmType.bodyLg, color = CalmWhite, modifier = Modifier.weight(1f))
        CalmToggle(checked, onCheckedChange)
    }
    ThinDivider()
}

@Composable
private fun OptionRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = CalmType.bodyLg, color = CalmWhite, modifier = Modifier.weight(1f))
        Text(text = value, style = CalmType.bodyMd, color = CalmGrayDim)
    }
    ThinDivider()
}

@Composable
private fun SummaryBlock(title: String, lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackSm)
            .background(CalmSurfaceContainer.copy(alpha = 0.85f))
            .padding(Spacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(Spacing.stackXs),
    ) {
        Text(text = title, style = CalmType.bodyLg, color = CalmWhite, fontWeight = FontWeight.SemiBold)
        lines.forEach { line ->
            Text(text = line, style = CalmType.bodyMd, color = CalmGray)
        }
    }
}

@Composable
private fun HistoryRow(day: DailyUsageRecord, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFF1F2530) else Color.Transparent)
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = dayLabel(day.dayStartEpochMs), style = CalmType.bodyLg, color = CalmWhite)
        Text(text = formatMinutes(day.totalScreenTimeMinutes), style = CalmType.bodyMd, color = CalmGray)
    }
    ThinDivider()
}

@Composable
private fun UsageRow(label: String, duration: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
    ) {
        Text(text = label, style = CalmType.bodyLg, color = CalmWhite, modifier = Modifier.weight(1f))
        Text(text = duration, style = CalmType.bodyMd, color = CalmGray)
    }
    ThinDivider()
}

@Composable
private fun HeatmapRows(title: String, rows: List<HeatmapRow>) {
    SummaryBlock(title, rows.map { "${it.label}  ${it.intensity}" })
}

@Composable
private fun AppHeatmap(apps: List<AppUsageRecord>, history: List<AppUsageRecord>) {
    if (apps.isEmpty()) return
    val days = history.map { it.dayStartEpochMs }.distinct().sorted()
    SummaryBlock(
        title = "App Heatmap",
        lines = apps.map { app ->
            val pattern = days.joinToString(" ") { day ->
                val minutes = history.firstOrNull { it.packageName == app.packageName && it.dayStartEpochMs == day }?.usageMinutes ?: 0
                intensityChar(minutes, history.filter { it.packageName == app.packageName }.map { it.usageMinutes })
            }
            "${app.appName.padEnd(12, ' ')} $pattern"
        },
    )
}

private data class HeatmapRow(val label: String, val intensity: String)

private fun AnalyticsRange.label(): String = when (this) {
    AnalyticsRange.TODAY -> "Today"
    AnalyticsRange.SEVEN_DAYS -> "7 Days"
    AnalyticsRange.THIRTY_DAYS -> "30 Days"
    AnalyticsRange.NINETY_DAYS -> "90 Days"
    AnalyticsRange.YEAR -> "365 Days"
}

private fun AnalyticsRange.next(): AnalyticsRange = when (this) {
    AnalyticsRange.TODAY -> AnalyticsRange.SEVEN_DAYS
    AnalyticsRange.SEVEN_DAYS -> AnalyticsRange.THIRTY_DAYS
    AnalyticsRange.THIRTY_DAYS -> AnalyticsRange.NINETY_DAYS
    AnalyticsRange.NINETY_DAYS -> AnalyticsRange.YEAR
    AnalyticsRange.YEAR -> AnalyticsRange.TODAY
}

private fun UsageSortOrder.label(): String = when (this) {
    UsageSortOrder.MOST_USED -> "Most Used"
    UsageSortOrder.LEAST_USED -> "Least Used"
    UsageSortOrder.ALPHABETICAL -> "Alphabetical"
}

private fun UsageSortOrder.next(): UsageSortOrder = when (this) {
    UsageSortOrder.MOST_USED -> UsageSortOrder.LEAST_USED
    UsageSortOrder.LEAST_USED -> UsageSortOrder.ALPHABETICAL
    UsageSortOrder.ALPHABETICAL -> UsageSortOrder.MOST_USED
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m"
        else -> "<1m"
    }
}

private fun comparisonLine(today: Int, yesterday: Int, label: String): String {
    val delta = today - yesterday
    return when {
        delta > 0 -> "$delta more $label than yesterday"
        delta < 0 -> "${-delta} less $label than yesterday"
        else -> "Same $label as yesterday"
    }
}

private fun dayLabel(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    val day = cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SHORT, java.util.Locale.getDefault()) ?: "Day"
    val month = cal.getDisplayName(java.util.Calendar.MONTH, java.util.Calendar.SHORT, java.util.Locale.getDefault()) ?: "Mon"
    return "$day $month ${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
}

private fun dayName(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    return cal.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, java.util.Locale.getDefault()) ?: "Day"
}

private fun weeklyLabels(): List<String> = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday",
)

private fun monthlySummaryLines(days: List<DailyUsageRecord>): List<String> {
    val currentMonth = days.takeLast(31)
    val previousMonth = days.dropLast(31).takeLast(31)
    val currentAvg = currentMonth.takeIf { it.isNotEmpty() }?.averageOf { it.totalScreenTimeMinutes } ?: 0.0
    val previousAvg = previousMonth.takeIf { it.isNotEmpty() }?.averageOf { it.totalScreenTimeMinutes } ?: 0.0
    val diff = currentAvg - previousAvg
    return listOf(
        "Current Month Average: ${formatMinutes(currentAvg.toInt())}",
        "Previous Month Average: ${formatMinutes(previousAvg.toInt())}",
        if (diff >= 0) "${formatMinutes(diff.toInt())} more than previous month" else "${formatMinutes((-diff).toInt())} less than previous month",
        "Days Meeting Goal: ${currentMonth.count { it.totalScreenTimeMinutes <= 180 }}",
        "Total Unlocks: ${currentMonth.sumOf { it.unlockCount }}",
        "Total Notifications: ${currentMonth.sumOf { it.notificationCount }}",
    )
}

private fun trendLines(days: List<DailyUsageRecord>): List<String> {
    val recent = days.takeLast(7)
    val previous = days.dropLast(7).takeLast(7)
    val recentAvg = recent.takeIf { it.isNotEmpty() }?.averageOf { it.totalScreenTimeMinutes } ?: 0.0
    val previousAvg = previous.takeIf { it.isNotEmpty() }?.averageOf { it.totalScreenTimeMinutes } ?: 0.0
    val unlockDelta = recent.sumOf { it.unlockCount } - previous.sumOf { it.unlockCount }
    val notificationDelta = recent.sumOf { it.notificationCount } - previous.sumOf { it.notificationCount }
    return listOf(
        if (previousAvg == 0.0) "Screen time trend is building." else "Screen time changed by ${percentChange(recentAvg, previousAvg)}% this week.",
        if (unlockDelta >= 0) "Unlocks are up $unlockDelta this week." else "Unlocks are down ${-unlockDelta} this week.",
        if (notificationDelta >= 0) "Notifications are up $notificationDelta this week." else "Notifications are down ${-notificationDelta} this week.",
    )
}

private fun reflectionLines(days: List<DailyUsageRecord>): List<String> {
    val recent = days.takeLast(7)
    if (recent.isEmpty()) return listOf("Weekly summary will appear as analytics accumulate.")
    val bestDay = recent.minByOrNull { it.totalScreenTimeMinutes }
    val worstDay = recent.maxByOrNull { it.totalScreenTimeMinutes }
    return listOf(
        "Average Screen Time: ${formatMinutes(recent.averageOf { it.totalScreenTimeMinutes }.toInt())}/day",
        "Best Day: ${bestDay?.let { dayName(it.dayStartEpochMs) } ?: "None"}",
        "Worst Day: ${worstDay?.let { dayName(it.dayStartEpochMs) } ?: "None"}",
        "Total Unlocks: ${recent.sumOf { it.unlockCount }}",
    )
}

private fun notificationSummaryLines(notifications: List<NotificationRecord>): List<String> {
    val posted = notifications.count { it.eventType == NotificationEventType.POSTED }
    val opened = notifications.count { it.eventType == NotificationEventType.OPENED }
    val ignored = notifications.count { it.eventType == NotificationEventType.IGNORED }
    return listOf(
        "Posted: $posted",
        "Opened: $opened",
        "Ignored: $ignored",
    )
}

private fun notificationCountsByApp(notifications: List<NotificationRecord>): List<Pair<String, Int>> =
    notifications.groupingBy { it.packageName }.eachCount().entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }

private fun unlockHourRows(unlocks: List<UnlockRecord>): List<Pair<String, Int>> =
    (0 until 24).map { hour ->
        val count = unlocks.count { unlock ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = unlock.timestampEpochMs }
            cal.get(java.util.Calendar.HOUR_OF_DAY) == hour
        }
        "%02d:00 - %02d:00".format(hour, (hour + 1) % 24) to count
    }

private fun sessionSummaryLines(today: List<UsageSessionRecord>, week: List<UsageSessionRecord>): List<String> {
    val average = week.takeIf { it.isNotEmpty() }?.averageOf { it.durationMinutes } ?: 0.0
    val shortSessions = week.count { it.durationMinutes in 1..2 }
    val bingeSessions = week.count { it.durationMinutes >= 30 }
    return listOf(
        "Longest Session Today: ${formatMinutes(today.maxOfOrNull { it.durationMinutes } ?: 0)}",
        "Longest Session This Week: ${formatMinutes(week.maxOfOrNull { it.durationMinutes } ?: 0)}",
        "Average Session Length: ${formatMinutes(average.toInt())}",
        "Frequent Short Sessions: $shortSessions",
        "Long Binge Sessions: $bingeSessions",
    )
}

private fun categorySummary(apps: List<AppUsageRecord>): List<String> {
    val totals = apps.groupingBy { it.category }.fold(0) { acc, app -> acc + app.usageMinutes }
    val total = totals.values.sum().coerceAtLeast(1)
    return AnalyticsCategory.entries.map { category ->
        val minutes = totals[category] ?: 0
        "${category.name.lowercase().replaceFirstChar { it.titlecase() }}: ${percent(minutes.toDouble(), total.toDouble())}%"
    }
}

private fun intensityChar(value: Int, allValues: List<Int>): String {
    val max = allValues.maxOrNull()?.coerceAtLeast(1) ?: 1
    val ratio = value.toDouble() / max.toDouble()
    return when {
        ratio <= 0.10 -> "░"
        ratio <= 0.35 -> "▒"
        ratio <= 0.65 -> "▓"
        else -> "█"
    }
}

private fun percentChange(current: Double, previous: Double): Int {
    if (previous <= 0.0) return 0
    return (((current - previous) / previous) * 100.0).toInt()
}

private fun percent(part: Double, whole: Double): Int = if (whole <= 0.0) 0 else ((part / whole) * 100.0).toInt()

private fun <T> List<T>.averageOf(selector: (T) -> Int): Double = if (isEmpty()) 0.0 else sumOf(selector).toDouble() / size.toDouble()

private fun isWithinDays(dayStartEpochMs: Long, days: Int): Boolean {
    val now = System.currentTimeMillis()
    val start = now - days * 24L * 60L * 60L * 1000L
    return dayStartEpochMs >= start
}

private fun nextRetention(current: Int): Int = when (current) {
    30 -> 90
    90 -> 365
    365 -> 730
    else -> 30
}

private fun shareAnalytics(context: android.content.Context, snapshot: AnalyticsDashboardSnapshot, exportJson: Boolean) {
    val text = if (exportJson) buildJson(snapshot) else buildCsv(snapshot)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, if (exportJson) "calm-analytics.json" else "calm-analytics.csv")
    }
    context.startActivity(Intent.createChooser(intent, "Export Analytics"))
}

private fun buildCsv(snapshot: AnalyticsDashboardSnapshot): String = buildString {
    appendLine("day,screen_time_minutes,unlocks,notifications,longest_session_minutes,app_launches")
    snapshot.dailyHistory.forEach {
        appendLine("${it.dayStartEpochMs},${it.totalScreenTimeMinutes},${it.unlockCount},${it.notificationCount},${it.longestSessionMinutes},${it.appLaunchCount}")
    }
    appendLine()
    appendLine("app_name,package_name,day_start,usage_minutes,launch_count,category")
    snapshot.appUsage.forEach {
        appendLine("${it.appName},${it.packageName},${it.dayStartEpochMs},${it.usageMinutes},${it.launchCount},${it.category}")
    }
}

private fun buildJson(snapshot: AnalyticsDashboardSnapshot): String = buildString {
    append('{')
    append("\"daily\":[")
    snapshot.dailyHistory.forEachIndexed { index, day ->
        if (index > 0) append(',')
        append("{\"dayStart\":${day.dayStartEpochMs},\"screenTime\":${day.totalScreenTimeMinutes},\"unlocks\":${day.unlockCount},\"notifications\":${day.notificationCount},\"longestSession\":${day.longestSessionMinutes},\"appLaunches\":${day.appLaunchCount}}")
    }
    append("],\"apps\":[")
    snapshot.appUsage.forEachIndexed { index, app ->
        if (index > 0) append(',')
        append("{\"dayStart\":${app.dayStartEpochMs},\"packageName\":\"${app.packageName}\",\"appName\":\"${app.appName}\",\"usageMinutes\":${app.usageMinutes},\"launchCount\":${app.launchCount},\"category\":\"${app.category}\"}")
    }
    append("]}")
}

