package com.calmlauncher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AnalyticsDashboardSnapshot
import com.calmlauncher.domain.model.AnalyticsRange
import com.calmlauncher.domain.model.AppUsageRecord
import com.calmlauncher.domain.model.DailyUsageRecord
import com.calmlauncher.domain.model.UsageSortOrder
import com.calmlauncher.domain.repository.AnalyticsRepository
import com.calmlauncher.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val usageAccessGranted: Boolean = false,
    val retentionDays: Int = 365,
    val selectedRange: AnalyticsRange = AnalyticsRange.SEVEN_DAYS,
    val sortOrder: UsageSortOrder = UsageSortOrder.MOST_USED,
    val snapshot: AnalyticsDashboardSnapshot = emptyAnalyticsSnapshot(),
    val selectedDayStartEpochMs: Long? = null,
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // A week is what the trend chart shows; loading a year of rows by default was wasted work.
    private val selectedRange = MutableStateFlow(AnalyticsRange.SEVEN_DAYS)
    private val selectedSortOrder = MutableStateFlow(UsageSortOrder.MOST_USED)
    private val selectedDayStart = MutableStateFlow<Long?>(null)
    private val usageAccessGranted = MutableStateFlow(false)

    init {
        refresh()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        settingsRepository.settings,
        selectedRange,
        selectedSortOrder,
        selectedDayStart,
        usageAccessGranted,
    ) { settings, range, sortOrder, selectedDayStart, usageAccessGranted ->
        AnalyticsQuery(settings.collectUsageAnalyticsEnabled, usageAccessGranted, settings.analyticsRetentionDays, range, sortOrder, selectedDayStart)
    }.flatMapLatest { query ->
        analyticsRepository.observeDashboard(daysForRange(query.range)).map { snapshot ->
            AnalyticsUiState(
                collectionEnabled = query.collectionEnabled,
                usageAccessGranted = query.usageAccessGranted,
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
        usageAccessGranted.value = analyticsRepository.hasUsageAccess()
        viewModelScope.launch {
            analyticsRepository.refresh()
            usageAccessGranted.value = analyticsRepository.hasUsageAccess()
        }
    }

    fun clearHistory() {
        viewModelScope.launch { analyticsRepository.clearHistory() }
    }

    fun selectedDay(snapshot: AnalyticsDashboardSnapshot): DailyUsageRecord =
        snapshot.dailyHistory.firstOrNull { it.dayStartEpochMs == selectedDayStart.value }
            ?: snapshot.today

    /** Per-app usage for a single day — the breakdown behind the selected bar in the chart. */
    fun appsForDay(
        snapshot: AnalyticsDashboardSnapshot,
        dayStartEpochMs: Long,
        sortOrder: UsageSortOrder,
    ): List<AppUsageRecord> = snapshot.appUsage
        .filter { it.dayStartEpochMs == dayStartEpochMs && it.usageMinutes > 0 }
        .sortedWith(sortOrder.comparator())

    private data class AnalyticsQuery(
        val collectionEnabled: Boolean,
        val usageAccessGranted: Boolean,
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
