package com.calmlauncher.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calmlauncher.core.designsystem.component.CalmBackBar
import com.calmlauncher.core.designsystem.component.CalmScaffold
import com.calmlauncher.core.designsystem.component.CalmToggle
import com.calmlauncher.core.designsystem.component.SectionLabel
import com.calmlauncher.core.designsystem.component.SettingRow
import com.calmlauncher.core.designsystem.component.ThinDivider
import com.calmlauncher.core.designsystem.theme.CalmGray
import com.calmlauncher.core.designsystem.theme.CalmGrayDim
import com.calmlauncher.core.designsystem.theme.CalmType
import com.calmlauncher.core.designsystem.theme.CalmWhite
import com.calmlauncher.core.designsystem.theme.Spacing
import com.calmlauncher.domain.model.AnalyticsDashboardSnapshot
import com.calmlauncher.domain.model.AnalyticsRange
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.UsageSortOrder
import java.util.Calendar
import java.util.Locale

/** How many apps to list before it stops being a summary and starts being a log. */
private const val TopAppLimit = 8

/**
 * Screen Time: today's total up top as the one number that matters, a seven-day bar chart
 * you can tap to change the day below it, and a per-app breakdown for whichever day is
 * selected. Monochrome throughout — bars are white, the selected one stays white while the
 * rest drop to grey, which is the only emphasis this screen needs.
 *
 * Every figure comes from [AnalyticsViewModel], which re-derives a rolling window of history
 * straight from Android's usage events on each resume, so a day the launcher wasn't opened
 * still fills in rather than reading as a genuine zero.
 */
@Composable
fun ScreenTimeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedDay = viewModel.selectedDay(state.snapshot)
    val selectedDayApps = viewModel.appsForDay(state.snapshot, selectedDay.dayStartEpochMs, state.sortOrder)
    val weekDays = state.snapshot.dailyHistory.takeLast(7)

    // Usage accrues while the user is elsewhere, so re-read on every return to this screen.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            when {
                !state.usageAccessGranted -> BlockingNotice(
                    text = "Screen time needs Usage Access. Until it's granted every figure " +
                        "here stays at zero.",
                    actionTitle = "Grant Usage Access",
                    onAction = { openUsageAccessSettings(context) },
                )

                !state.collectionEnabled -> Notice(
                    "Usage collection is off. Nothing new is being recorded.",
                )
            }

            // --- Today -------------------------------------------------------------------
            HeroTotal(
                minutes = state.snapshot.today.totalScreenTimeMinutes,
                comparison = comparisonLine(
                    today = state.snapshot.today.totalScreenTimeMinutes,
                    yesterday = state.snapshot.yesterday.totalScreenTimeMinutes,
                ),
            )

            // --- Week --------------------------------------------------------------------
            SectionLabel("Last 7 days")
            WeeklyTrendChart(
                days = weekDays,
                selectedDayStartEpochMs = selectedDay.dayStartEpochMs,
                onSelectDay = viewModel::selectDay,
            )
            SettingRow(
                title = "Daily average",
                value = formatMinutes(averageMinutes(weekDays)),
            )

            // --- Selected day ------------------------------------------------------------
            SectionLabel(dayLabel(selectedDay.dayStartEpochMs))
            SettingRow(title = "Screen time", value = formatMinutes(selectedDay.totalScreenTimeMinutes))
            SettingRow(title = "Unlocks", value = selectedDay.unlockCount.toString())
            SettingRow(title = "Notifications", value = selectedDay.notificationCount.toString())
            SettingRow(title = "Longest session", value = formatMinutes(selectedDay.longestSessionMinutes))
            SettingRow(title = "App opens", value = selectedDay.appLaunchCount.toString())

            SectionLabel("Apps")
            SettingRow(
                title = "Sort",
                value = state.sortOrder.label(),
                onClick = { viewModel.selectSortOrder(state.sortOrder.next()) },
            )
            if (selectedDayApps.isEmpty()) {
                Notice("No app usage recorded for this day.")
            } else {
                val busiest = selectedDayApps.maxOf { it.usageMinutes }.coerceAtLeast(1)
                selectedDayApps.take(TopAppLimit).forEach { app ->
                    AppUsageRow(
                        label = app.appName,
                        minutes = app.usageMinutes,
                        fraction = app.usageMinutes.toFloat() / busiest.toFloat(),
                    )
                }
            }

            // --- Data --------------------------------------------------------------------
            SectionLabel("Data")
            SettingRow(
                title = "Collect usage data",
                trailing = {
                    CalmToggle(state.collectionEnabled, onCheckedChange = viewModel::toggleCollection)
                },
            )
            SettingRow(
                title = "Range",
                value = state.selectedRange.label(),
                onClick = { viewModel.selectRange(state.selectedRange.next()) },
            )
            SettingRow(
                title = "Export CSV",
                onClick = { shareAnalytics(context, state.snapshot) },
                showChevron = true,
            )
            SettingRow(
                title = "Clear history",
                onClick = viewModel::clearHistory,
                destructive = true,
            )

            Spacer(Modifier.height(Spacing.stackLg))
        }
    }
}

/** Today's total, set at headline size — the one figure worth walking away with. */
@Composable
private fun HeroTotal(minutes: Int, comparison: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.stackLg),
    ) {
        Text(text = "TODAY", style = CalmType.labelLg, color = CalmGray)
        Text(
            text = formatMinutes(minutes),
            style = CalmType.headlineLgMobile,
            color = CalmWhite,
            modifier = Modifier.padding(top = Spacing.stackSm),
        )
        Text(
            text = comparison,
            style = CalmType.bodyMd,
            color = CalmGray,
            modifier = Modifier.padding(top = Spacing.stackSm),
        )
    }
    ThinDivider()
}

/**
 * Seven plain bars, one per day, scaled against the busiest day in view. Tapping a bar picks
 * the day the breakdown below refers to; the selected bar and its label stay white while the
 * rest recede to grey.
 */
@Composable
private fun WeeklyTrendChart(
    days: List<DailyUsageRecord>,
    selectedDayStartEpochMs: Long,
    onSelectDay: (Long) -> Unit,
) {
    if (days.isEmpty()) {
        Notice("No history yet — check back tomorrow.")
        return
    }
    val maxMinutes = days.maxOf { it.totalScreenTimeMinutes }.coerceAtLeast(1)
    val maxBarHeight = 120.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
        horizontalArrangement = Arrangement.spacedBy(Spacing.stackMd),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { day ->
            val selected = day.dayStartEpochMs == selectedDayStartEpochMs
            val fraction = (day.totalScreenTimeMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
            val interaction = remember(day.dayStartEpochMs) { MutableInteractionSource() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onSelectDay(day.dayStartEpochMs) },
                    ),
            ) {
                // A zero-usage day still shows a 2dp stub, so the bar reads as "no time"
                // rather than as a missing day.
                Spacer(Modifier.height(maxBarHeight * (1f - fraction)))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((maxBarHeight * fraction).coerceAtLeast(2.dp))
                        .background(if (selected) CalmWhite else CalmGrayDim),
                )
                Text(
                    text = dayInitial(day.dayStartEpochMs),
                    style = CalmType.labelMd,
                    color = if (selected) CalmWhite else CalmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = Spacing.base),
                )
            }
        }
    }
    ThinDivider()
}

/**
 * One app's time for the day, with a hairline bar underneath showing its share of the
 * busiest app. The bar is the only ranking cue; there are no colours to decode.
 */
@Composable
private fun AppUsageRow(label: String, minutes: Int, fraction: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.marginMobile, vertical = Spacing.rowVertical),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.gutter),
        ) {
            Text(
                text = label,
                style = CalmType.bodyLg,
                color = CalmWhite,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(text = formatMinutes(minutes), style = CalmType.bodyMd, color = CalmGray)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(2.dp)
                .background(CalmGrayDim),
        )
    }
    ThinDivider()
}

@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        style = CalmType.bodyMd,
        color = CalmGray,
        modifier = Modifier.padding(
            horizontal = Spacing.marginMobile,
            vertical = Spacing.rowVertical,
        ),
    )
}

@Composable
private fun BlockingNotice(text: String, actionTitle: String, onAction: () -> Unit) {
    Notice(text)
    SettingRow(title = actionTitle, onClick = onAction, showChevron = true)
}

private fun averageMinutes(days: List<DailyUsageRecord>): Int =
    if (days.isEmpty()) 0 else days.sumOf { it.totalScreenTimeMinutes } / days.size

private fun AnalyticsRange.label(): String = when (this) {
    AnalyticsRange.TODAY -> "Today"
    AnalyticsRange.SEVEN_DAYS -> "7 days"
    AnalyticsRange.THIRTY_DAYS -> "30 days"
    AnalyticsRange.NINETY_DAYS -> "90 days"
    AnalyticsRange.YEAR -> "365 days"
}

private fun AnalyticsRange.next(): AnalyticsRange = when (this) {
    AnalyticsRange.TODAY -> AnalyticsRange.SEVEN_DAYS
    AnalyticsRange.SEVEN_DAYS -> AnalyticsRange.THIRTY_DAYS
    AnalyticsRange.THIRTY_DAYS -> AnalyticsRange.NINETY_DAYS
    AnalyticsRange.NINETY_DAYS -> AnalyticsRange.YEAR
    AnalyticsRange.YEAR -> AnalyticsRange.TODAY
}

private fun UsageSortOrder.label(): String = when (this) {
    UsageSortOrder.MOST_USED -> "Most used"
    UsageSortOrder.LEAST_USED -> "Least used"
    UsageSortOrder.ALPHABETICAL -> "A–Z"
}

private fun UsageSortOrder.next(): UsageSortOrder = when (this) {
    UsageSortOrder.MOST_USED -> UsageSortOrder.LEAST_USED
    UsageSortOrder.LEAST_USED -> UsageSortOrder.ALPHABETICAL
    UsageSortOrder.ALPHABETICAL -> UsageSortOrder.MOST_USED
}

/** "1h 12m" / "12m" / "0m". Zero really is zero — not "<1m". */
private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun comparisonLine(today: Int, yesterday: Int): String {
    val delta = today - yesterday
    return when {
        yesterday == 0 -> "No figure for yesterday to compare against"
        delta > 0 -> "${formatMinutes(delta)} more than yesterday"
        delta < 0 -> "${formatMinutes(-delta)} less than yesterday"
        else -> "Same as yesterday"
    }
}

private fun dayLabel(epochMs: Long): String {
    if (epochMs <= 0L) return "Today"
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val today = Calendar.getInstance()
    if (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    ) {
        return "Today"
    }
    val day = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "Day"
    val month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
    return "$day ${cal.get(Calendar.DAY_OF_MONTH)} $month"
}

/** Single-letter weekday for the chart axis, e.g. "M" for Monday. */
private fun dayInitial(epochMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
    val name = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        ?: return "?"
    return name.take(1).uppercase(Locale.getDefault())
}

private fun shareAnalytics(context: android.content.Context, snapshot: AnalyticsDashboardSnapshot) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_TEXT, buildCsv(snapshot))
        putExtra(Intent.EXTRA_SUBJECT, "calm-analytics.csv")
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Export Analytics")) }
}

private fun openUsageAccessSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun buildCsv(snapshot: AnalyticsDashboardSnapshot): String = buildString {
    appendLine("day,screen_time_minutes,unlocks,notifications,longest_session_minutes,app_launches")
    snapshot.dailyHistory.forEach {
        appendLine("${it.dayStartEpochMs},${it.totalScreenTimeMinutes},${it.unlockCount},${it.notificationCount},${it.longestSessionMinutes},${it.appLaunchCount}")
    }
    appendLine()
    appendLine("app_name,package_name,day_start,usage_minutes,launch_count,category")
    snapshot.appUsage.forEach {
        // Labels can contain commas ("Docs, Sheets & Slides"), so quote the free-text column.
        appendLine("\"${it.appName.replace("\"", "\"\"")}\",${it.packageName},${it.dayStartEpochMs},${it.usageMinutes},${it.launchCount},${it.category}")
    }
}
