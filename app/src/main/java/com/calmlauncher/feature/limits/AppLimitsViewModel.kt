package com.calmlauncher.feature.limits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
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
import kotlinx.coroutines.flow.first
import com.calmlauncher.work.AppLimitEnforceWorker
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
    val groupAssignments: Map<String, String> = emptyMap(),
)

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val appLimitRepository: AppLimitRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    private val apps = appRepository.observeApps()
    private val rules = appLimitRepository.observeRules()
    private val groupAssignments = appLimitRepository.observeGroupAssignments()
    private val usage = appLimitRepository.observeTodayUsage()
    private val events = appLimitRepository.observeTodayEvents()

    val uiState: StateFlow<AppLimitsUiState> = combine(
        apps,
        rules,
        groupAssignments,
        usage,
        events,
    ) { apps, rules, groupAssignments, usage, events ->
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
            groupAssignments = groupAssignments.associate { it.packageName to it.groupId },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppLimitsUiState(),
    )

    init {
        viewModelScope.launch { appLimitRepository.refreshUsageSnapshot() }
    }

    fun saveLimit(packageName: String, enabled: Boolean, limitMinutes: Int) {
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
            // Schedule enforcement when the preset limit is expected to expire.
            if (enabled) scheduleEnforceForPreset(packageName, limitMinutes)
        }
    }

    fun saveGroupLimit(groupId: String, packageNames: Set<String>, enabled: Boolean, limitMinutes: Int) {
        viewModelScope.launch {
            appLimitRepository.saveGroupAssignments(groupId, packageNames)
            packageNames.forEach { packageName ->
                appLimitRepository.saveRule(
                    AppLimitRule(
                        packageName = packageName,
                        enabled = enabled,
                        dailyLimitMinutes = limitMinutes,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
                if (enabled) scheduleEnforceForPreset(packageName, limitMinutes)
            }
            appLimitRepository.refreshUsageSnapshot()
        }
    }

    fun setEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            appLimitRepository.setEnabled(packageName, enabled)
            appLimitRepository.refreshUsageSnapshot()
            if (enabled) {
                // When enabling, schedule enforcement based on current usage.
                val rule = appLimitRepository.currentRule(packageName)
                rule?.dailyLimitMinutes?.let { scheduleEnforceForPreset(packageName, it) }
            } else {
                cancelEnforceWork(packageName)
            }
        }
    }

    fun removeLimit(packageName: String) {
        viewModelScope.launch {
            appLimitRepository.deleteRule(packageName)
            appLimitRepository.refreshUsageSnapshot()
            cancelEnforceWork(packageName)
        }
    }

    fun grantOverride(packageName: String, minutes: Int) {
        viewModelScope.launch {
            appLimitRepository.extendOverride(packageName, minutes)
            appLimitRepository.refreshUsageSnapshot()
            // After extending, schedule a worker at the override expiry to enforce closing the app.
            val rule = appLimitRepository.currentRule(packageName)
            val now = System.currentTimeMillis()
            val delay = rule?.overrideUntilEpochMs?.let { (it - now).coerceAtLeast(0L) } ?: 0L
            if (delay >= 0L) scheduleEnforceWork(packageName, delay)
        }
    }

    private fun scheduleEnforceForPreset(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            appLimitRepository.refreshUsageSnapshot()
            val usage = appLimitRepository.observeTodayUsage().first()
            val usedMs = usage.find { it.packageName == packageName }?.usedMs ?: 0L
            val limitMs = limitMinutes * 60_000L
            val remaining = (limitMs - usedMs).coerceAtLeast(0L)
            scheduleEnforceWork(packageName, remaining)
        }
    }

    private fun scheduleEnforceWork(packageName: String, delayMs: Long) {
        val data = Data.Builder().putString("packageName", packageName).build()
        val request = OneTimeWorkRequestBuilder<AppLimitEnforceWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork("app_limit_enforce_$packageName", ExistingWorkPolicy.REPLACE, request)
    }

    private fun cancelEnforceWork(packageName: String) {
        workManager.cancelUniqueWork("app_limit_enforce_$packageName")
    }
}
