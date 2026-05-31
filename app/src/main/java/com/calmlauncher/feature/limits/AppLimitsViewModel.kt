package com.calmlauncher.feature.limits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppLimitRowUiState(
    val app: AppEntry,
    val rule: AppLimitRule?,
    val usedMinutes: Int,
    val blockedToday: Boolean,
    val overrideActive: Boolean,
) {
    val limitMinutes: Int? get() = rule?.dailyLimitMinutes
}

data class AppLimitsUiState(
    val summary: AppLimitSummary = AppLimitSummary(),
    val apps: List<AppLimitRowUiState> = emptyList(),
)

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val appLimitRepository: AppLimitRepository,
) : ViewModel() {

    private val apps = appRepository.observeApps()
    private val rules = appLimitRepository.observeRules()
    private val usage = appLimitRepository.observeTodayUsage()
    private val events = appLimitRepository.observeTodayEvents()

    val uiState: StateFlow<AppLimitsUiState> = combine(
        apps,
        rules,
        usage,
        events,
    ) { apps, rules, usage, events ->
        val ruleByPackage = rules.associateBy { it.packageName }
        val usageByPackage = usage.associateBy { it.packageName }
        val blockedEvents = events.filter { it.eventType == AppLimitEventType.BLOCKED }
        val blockedCounts = blockedEvents.groupingBy { it.packageName }.eachCount()
        val top = blockedCounts.maxByOrNull { it.value }

        AppLimitsUiState(
            summary = AppLimitSummary(
                blockedLaunchesToday = blockedEvents.size,
                limitedAppsToday = blockedCounts.size,
                estimatedTimeSavedMinutes = blockedEvents.size * 10,
                topLimitedPackage = top?.key,
                topLimitedCount = top?.value ?: 0,
            ),
            apps = apps.map { app ->
                val rule = ruleByPackage[app.packageName]
                val usedMinutes = ((usageByPackage[app.packageName]?.usedMs ?: 0L) / 60_000L).toInt()
                AppLimitRowUiState(
                    app = app,
                    rule = rule,
                    usedMinutes = usedMinutes,
                    blockedToday = blockedEvents.any { it.packageName == app.packageName },
                    overrideActive = rule?.overrideUntilEpochMs?.let { it > System.currentTimeMillis() } == true,
                )
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppLimitsUiState(),
    )

    init {
        viewModelScope.launch { appLimitRepository.refreshUsageSnapshot() }
    }

    fun saveLimit(packageName: String, label: String, enabled: Boolean, limitMinutes: Int) {
        viewModelScope.launch {
            appLimitRepository.saveRule(
                AppLimitRule(
                    packageName = packageName,
                    enabled = enabled,
                    dailyLimitMinutes = limitMinutes,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            appLimitRepository.refreshUsageSnapshot()
        }
    }

    fun setEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            appLimitRepository.setEnabled(packageName, enabled)
            appLimitRepository.refreshUsageSnapshot()
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            appLimitRepository.deleteRule(packageName)
            appLimitRepository.refreshUsageSnapshot()
        }
    }

    fun grantOverride(packageName: String, minutes: Int) {
        viewModelScope.launch {
            appLimitRepository.extendOverride(packageName, minutes)
            appLimitRepository.refreshUsageSnapshot()
        }
    }
}
