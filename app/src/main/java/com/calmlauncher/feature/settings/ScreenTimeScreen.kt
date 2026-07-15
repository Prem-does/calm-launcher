package com.calmlauncher.feature.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmBlack
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmSurfaceContainer
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AnalyticsDashboardSnapshot
import com.calmlauncher.domain.model.AnalyticsRange
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppUsageRecord
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.NotificationEventType
import com.calmlauncher.domain.model.NotificationRecord
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
        topBar = { CalmBackBar(title = "Screen Time", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                title = "Range",
                value = state.selectedRange.label(),
                onClick = { viewModel.selectRange(state.selectedRange.next()) },
            )
            OptionRow(
                title = "Export CSV",
                value = "Share",
                onClick = { shareAnalytics(context, state.snapshot, exportJson = false) },
            )
            OptionRow(
                title = "Clear Analytics History",
                value = "Delete all",
                onClick = viewModel::clearHistory,
            )

            SectionLabel("Overview")
            SummaryBlock(
                title = "Today",
                lines = listOf(
                    "Screen Time: ${formatMinutes(state.snapshot.today.totalScreenTimeMinutes)}",
                    "Most Used App: ${topApps.firstOrNull()?.appName ?: "None"}",
                ),
            )

            SectionLabel("Weekly Trend")
            WeeklyTrendChart(days = state.snapshot.dailyHistory.takeLast(7), appUsage = state.snapshot.appUsage)
            WeeklyTrendLegend()

            SectionLabel("Top Apps")
            topApps.take(5).forEach { app ->
                UsageRow(label = app.appName, duration = formatMinutes(app.usageMinutes))
            }

            SectionLabel("Selected Day")
            SummaryBlock(
                title = dayName(selectedDay.dayStartEpochMs),
                lines = listOf(
                    "Screen Time: ${formatMinutes(selectedDay.totalScreenTimeMinutes)}",
                    "Unlocks: ${selectedUnlocks.size}",
                    "Notifications: ${selectedNotifications.size}",
                    "Sessions: ${selectedSessions.size}",
                ),
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }

}

@Composable
private fun WeeklyTrendChart(days: List<DailyUsageRecord>, appUsage: List<AppUsageRecord>) {
    if (days.isEmpty()) {
        Text(text = "No weekly data", style = CalmType.bodyMd, color = CalmGray, modifier = Modifier.padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical))
        return
    }
    val maxMinutes = days.maxOfOrNull { it.totalScreenTimeMinutes }?.coerceAtLeast(1) ?: 1
    val maxBarHeight = 120.dp
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
    ) {
        days.forEach { day ->
            val fraction = (day.totalScreenTimeMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
            val barHeight = maxBarHeight * fraction
            val segments = categorySegments(
                appUsage = appUsage.filter { it.dayStartEpochMs == day.dayStartEpochMs },
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(maxBarHeight * (1f - fraction)))
                Column(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(6.dp)),
                ) {
                    segments.forEach { segment ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight * segment.fraction)
                                .background(appCategoryColor(segment.category)),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = dayName(day.dayStartEpochMs).take(3), style = CalmType.bodyMd, color = CalmGray)
            }
        }
    }
}

private data class CategoryUsageSegment(
    val category: AppCategory,
    val fraction: Float,
)

private fun categorySegments(appUsage: List<AppUsageRecord>): List<CategoryUsageSegment> {
    val totals = appUsage
        .groupingBy { it.category }
        .fold(0) { acc, app -> acc + app.usageMinutes }
    val totalMinutes = totals.values.sum()

    if (totalMinutes <= 0) {
        return listOf(CategoryUsageSegment(AppCategory.OTHER, 1f))
    }

    return AppCategory.entries.mapNotNull { category ->
        val minutes = totals[category] ?: 0
        if (minutes <= 0) null else CategoryUsageSegment(
            category = category,
            fraction = minutes.toFloat() / totalMinutes.toFloat(),
        )
    }
}

@Composable
private fun appCategoryColor(category: AppCategory): Color {
    val dark = isSystemInDarkTheme()
    val primary = if (dark) CalmWhite else CalmBlack
    val grey = CalmGray

    // White/Black for non-distracting apps: TOOL, COMMUNICATION, STORE, OTHER
    return when (category) {
        AppCategory.TOOL,
        AppCategory.COMMUNICATION,
        AppCategory.STORE,
        AppCategory.OTHER -> primary

        // Grey for distracting categories: SOCIAL, ENTERTAINMENT, GAME, BROWSER
        AppCategory.SOCIAL,
        AppCategory.ENTERTAINMENT,
        AppCategory.GAME,
        AppCategory.BROWSER -> grey
    }
}

private fun AppCategory.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun WeeklyTrendLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.gutter),
    ) {
        Text(text = "Legend:", style = CalmType.bodyMd, color = CalmGray)

        AppCategory.entries.toList().chunked(3).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowCategories.forEach { category ->
                    legendItem(appCategoryColor(category), category.label())
                }
            }
        }
    }
}

@Composable
private fun legendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Text(text = label, style = CalmType.bodyMd, color = CalmGrayDim)
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
        CalmToggle(checked = checked, onCheckedChange = onCheckedChange)
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
            .clip(RoundedCornerShape(20.dp))
            .background(CalmSurfaceContainer.copy(alpha = 0.85f))
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackSm)
            .padding(Spacing.marginMobile),
        verticalArrangement = Arrangement.spacedBy(Spacing.gutter),
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
    return AppCategory.entries.map { category ->
        val minutes = totals[category] ?: 0
        "${category.label()}: ${percent(minutes.toDouble(), total.toDouble())}%"
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

