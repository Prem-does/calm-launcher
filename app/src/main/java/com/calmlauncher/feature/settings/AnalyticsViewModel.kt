package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AnalyticsCategory
import com.calmlauncher.domain.model.AnalyticsDashboardSnapshot
import com.calmlauncher.domain.model.AnalyticsRange
import com.calmlauncher.domain.model.AppUsageRecord
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.NotificationEventType
import com.calmlauncher.domain.model.NotificationRecord
import com.calmlauncher.domain.model.UnlockRecord
import com.calmlauncher.domain.model.UsageSessionRecord
import com.calmlauncher.domain.model.UsageSortOrder
import com.calmlauncher.domain.repository.AnalyticsRepository
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val collectionEnabled: Boolean = true,
    val retentionDays: Int = 365,
    val selectedRange: AnalyticsRange = AnalyticsRange.YEAR,
    val sortOrder: UsageSortOrder = UsageSortOrder.MOST_USED,
    val snapshot: AnalyticsDashboardSnapshot = emptyAnalyticsSnapshot(),
    val selectedDayStartEpochMs: Long? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val selectedRange = MutableStateFlow(AnalyticsRange.YEAR)
    private val selectedSortOrder = MutableStateFlow(UsageSortOrder.MOST_USED)
    private val selectedDayStart = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        settingsRepository.settings,
        selectedRange,
        selectedSortOrder,
        selectedDayStart,
    ) { settings, range, sortOrder, selectedDayStart ->
        AnalyticsQuery(settings.collectUsageAnalyticsEnabled, settings.analyticsRetentionDays, range, sortOrder, selectedDayStart)
    }.flatMapLatest { query ->
        analyticsRepository.observeDashboard(daysForRange(query.range)).map { snapshot ->
            AnalyticsUiState(
                collectionEnabled = query.collectionEnabled,
                retentionDays = query.retentionDays,
                selectedRange = query.range,
                sortOrder = query.sortOrder,
                snapshot = snapshot,
                selectedDayStartEpochMs = query.selectedDayStartEpochMs ?: snapshot.today.dayStartEpochMs,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AnalyticsUiState(),
    )

    fun selectRange(range: AnalyticsRange) {
        selectedRange.value = range
    }

    fun selectSortOrder(order: UsageSortOrder) {
        selectedSortOrder.value = order
    }

    fun selectDay(dayStartEpochMs: Long) {
        selectedDayStart.value = dayStartEpochMs
    }

    fun toggleCollection(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(collectUsageAnalyticsEnabled = enabled) }
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(analyticsRetentionDays = days) }
            analyticsRepository.pruneOlderThan(days)
        }
    }

    fun refresh() {
        viewModelScope.launch { analyticsRepository.refresh() }
    }

    fun clearHistory() {
        viewModelScope.launch { analyticsRepository.clearHistory() }
    }

    fun selectedDay(snapshot: AnalyticsDashboardSnapshot): DailyUsageRecord =
        snapshot.dailyHistory.firstOrNull { it.dayStartEpochMs == selectedDayStart.value }
            ?: snapshot.today

    fun topApps(snapshot: AnalyticsDashboardSnapshot, sortOrder: UsageSortOrder): List<AppUsageRecord> =
        snapshot.appUsage
            .groupBy { it.packageName }
            .map { (packageName, rows) ->
                val first = rows.first()
                AppUsageRecord(
                    dayStartEpochMs = first.dayStartEpochMs,
                    packageName = packageName,
                    appName = first.appName,
                    category = first.category,
                    usageMinutes = rows.sumOf { it.usageMinutes },
                    launchCount = rows.sumOf { it.launchCount },
                )
            }
            .sortedWith(sortOrder.comparator())

    fun sessionsForDay(snapshot: AnalyticsDashboardSnapshot, dayStartEpochMs: Long): List<UsageSessionRecord> =
        snapshot.sessions.filter { it.dayStartEpochMs == dayStartEpochMs }

    fun unlocksForDay(snapshot: AnalyticsDashboardSnapshot, dayStartEpochMs: Long): List<UnlockRecord> =
        snapshot.unlocks.filter { it.dayStartEpochMs == dayStartEpochMs }

    fun notificationsForDay(snapshot: AnalyticsDashboardSnapshot, dayStartEpochMs: Long): List<NotificationRecord> =
        snapshot.notifications.filter { it.dayStartEpochMs == dayStartEpochMs }

    private data class AnalyticsQuery(
        val collectionEnabled: Boolean,
        val retentionDays: Int,
        val range: AnalyticsRange,
        val sortOrder: UsageSortOrder,
        val selectedDayStartEpochMs: Long?,
    )

    private fun daysForRange(range: AnalyticsRange): Int = when (range) {
        AnalyticsRange.TODAY -> 1
        AnalyticsRange.SEVEN_DAYS -> 7
        AnalyticsRange.THIRTY_DAYS -> 30
        AnalyticsRange.NINETY_DAYS -> 90
        AnalyticsRange.YEAR -> 365
    }

    private fun UsageSortOrder.comparator(): Comparator<AppUsageRecord> = when (this) {
        UsageSortOrder.MOST_USED -> compareByDescending<AppUsageRecord> { it.usageMinutes }.thenBy { it.appName }
        UsageSortOrder.LEAST_USED -> compareBy<AppUsageRecord> { it.usageMinutes }.thenBy { it.appName }
        UsageSortOrder.ALPHABETICAL -> compareBy<AppUsageRecord> { it.appName }
    }
}

private fun emptyAnalyticsSnapshot(): AnalyticsDashboardSnapshot = AnalyticsDashboardSnapshot(
    today = DailyUsageRecord(0L, 0, 0, 0, 0, 0),
    yesterday = DailyUsageRecord(0L, 0, 0, 0, 0, 0),
    dailyHistory = emptyList(),
    appUsage = emptyList(),
    sessions = emptyList(),
    unlocks = emptyList(),
    notifications = emptyList(),
)
