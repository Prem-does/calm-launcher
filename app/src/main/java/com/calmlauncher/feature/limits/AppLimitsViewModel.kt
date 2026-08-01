package com.calmlauncher.feature.limits

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.calmlauncher.accessibility.PlatformGuardPolicy
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.AppLimitExtensionCaps
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.OverrideDenialReason
import com.calmlauncher.domain.model.OverrideResult
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.calmlauncher.work.AppLimitEnforceWorker
import javax.inject.Inject

/** A single app row: its rule (if any), today's usage, and whether it's currently blocked. */
data class AppLimitRowUiState(
    val app: AppEntry,
    val rule: AppLimitRule?,
    val usedMinutes: Int,
    val blockedToday: Boolean,
    val overrideActive: Boolean,
) {
    val limitMinutes: Int? get() = rule?.dailyLimitMinutes

    /** Minutes left today, or null when this app has no active limit. */
    val remainingMinutes: Int?
        get() = rule?.takeIf { it.enabled }?.let { (it.dailyLimitMinutes - usedMinutes).coerceAtLeast(0) }
}

/**
 * One limit group as the screen renders it. Everything here is derived from real rules and
 * real usage — a group with no rule reports `limitMinutes = null` rather than borrowing a
 * suggested default, so the UI can never claim a limit that isn't actually in force.
 */
data class AppLimitGroupUiState(
    val id: String,
    val title: String,
    val apps: List<AppLimitRowUiState>,
    val limitMinutes: Int?,
    val limitEnabled: Boolean,
    val usedMinutes: Int,
    val suggestedLimitMinutes: Int,
) {
    val hasLimit: Boolean get() = limitMinutes != null
    val remainingMinutes: Int?
        get() = limitMinutes?.takeIf { limitEnabled }?.let { (it - usedMinutes).coerceAtLeast(0) }
}

data class AppLimitsUiState(
    val summary: AppLimitSummary = AppLimitSummary(),
    val usageAccessGranted: Boolean = true,
    /**
     * "Display over other apps". Optional, and limits still work without it — but an app that
     * runs out of time mid-use can then only be closed, with no chance to say why first.
     */
    val canShowBlockScreens: Boolean = true,
    val apps: List<AppLimitRowUiState> = emptyList(),
    val groups: List<AppLimitGroupUiState> = emptyList(),
) {
    /** Time spent today in apps that actually sit under an enabled limit. */
    val limitedMinutesUsed: Int
        get() = groups.filter { it.limitEnabled }.sumOf { it.usedMinutes }

    val groupsLimited: Int get() = groups.count { it.limitEnabled }
}

/** Static group definitions: title, the categories that seed it, and a sensible starting limit. */
private data class GroupDefinition(
    val id: String,
    val title: String,
    val categories: Set<AppCategory>,
    val suggestedLimitMinutes: Int,
)

private val GroupDefinitions = listOf(
    GroupDefinition("social", "Social Group", setOf(AppCategory.SOCIAL), 30),
    GroupDefinition(
        "entertainment",
        "Entertainment Group",
        setOf(AppCategory.ENTERTAINMENT, AppCategory.GAME),
        60,
    ),
    GroupDefinition("browser", "Web Browser", setOf(AppCategory.BROWSER), 45),
    GroupDefinition(
        "information",
        "Information Group",
        setOf(AppCategory.COMMUNICATION, AppCategory.TOOL, AppCategory.OTHER),
        60,
    ),
)

@HiltViewModel
class AppLimitsViewModel @Inject constructor(
    appRepository: AppRepository,
    private val appLimitRepository: AppLimitRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val applicationContext: Context,
) : ViewModel() {

    private val workManager by lazy { WorkManager.getInstance(applicationContext) }

    private val usageAccessGranted = MutableStateFlow(appLimitRepository.hasUsageAccess())
    private val canShowBlockScreens =
        MutableStateFlow(PlatformGuardPolicy.canDrawOverlays(applicationContext))

    /**
     * A refused extension, surfaced so the screen can say so. Emitting rather than swallowing is
     * the point: a silently ignored tap looks like a broken button and invites another try.
     */
    private val _overrideDenied = MutableSharedFlow<OverrideDenialReason>(extraBufferCapacity = 1)
    val overrideDenied: SharedFlow<OverrideDenialReason> = _overrideDenied.asSharedFlow()

    /** A cap increase that has been parked until the next daily reset. */
    private val _capChangeDeferred = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val capChangeDeferred: SharedFlow<Unit> = _capChangeDeferred.asSharedFlow()

    private val _extensionCaps = MutableStateFlow(DefaultCaps)

    /** The extension budget currently in force, for the caps section of the screen. */
    val extensionCaps: StateFlow<AppLimitExtensionCaps> = _extensionCaps.asStateFlow()

    init {
        refreshExtensionCaps()
    }

    /**
     * Re-read the caps in force.
     *
     * Called after any change rather than assuming the requested value took effect, because a
     * relaxation is deliberately deferred to the next daily reset — so the value the user picked and
     * the value now in force are legitimately different, and the screen has to show the latter.
     */
    private fun refreshExtensionCaps() {
        viewModelScope.launch {
            _extensionCaps.value = runCatching { appLimitRepository.currentExtensionCaps() }
                .getOrDefault(DefaultCaps)
        }
    }

    private val apps = appRepository.observeApps()
    private val rules = appLimitRepository.observeRules()
    private val groupAssignments = appLimitRepository.observeGroupAssignments()
    private val usage = appLimitRepository.observeTodayUsage()
    private val events = appLimitRepository.observeTodayEvents()

    private val limitData = combine(
        apps,
        rules,
        groupAssignments,
        usage,
        events,
    ) { apps, rules, groupAssignments, usage, events ->
        val ruleByPackage = rules.associateBy { it.packageName }
        val usageByPackage = usage.associateBy { it.packageName }
        val blockedEvents = events.filter { it.eventType == AppLimitEventType.BLOCKED }
        val blockedPackages = blockedEvents.map { it.packageName }.toSet()
        val blockedCounts = blockedEvents.groupingBy { it.packageName }.eachCount()
        val top = blockedCounts.maxByOrNull { it.value }
        val assignmentByPackage = groupAssignments.associate { it.packageName to it.groupId }
        val now = System.currentTimeMillis()

        val rows = apps.map { app ->
            val rule = ruleByPackage[app.packageName]
            AppLimitRowUiState(
                app = app,
                rule = rule,
                usedMinutes = ((usageByPackage[app.packageName]?.usedMs ?: 0L) / 60_000L).toInt(),
                blockedToday = app.packageName in blockedPackages,
                overrideActive = (rule?.overrideUntilEpochMs ?: 0L) > now,
            )
        }

        AppLimitsUiState(
            summary = AppLimitSummary(
                blockedLaunchesToday = blockedEvents.size,
                limitedAppsToday = blockedCounts.size,
                estimatedTimeSavedMinutes = blockedEvents.size * 10,
                topLimitedPackage = top?.key,
                topLimitedCount = top?.value ?: 0,
            ),
            apps = rows,
            groups = buildGroups(rows, assignmentByPackage),
        )
    }

    val uiState: StateFlow<AppLimitsUiState> = combine(
        limitData,
        usageAccessGranted,
        canShowBlockScreens,
    ) { data, granted, canOverlay ->
        data.copy(usageAccessGranted = granted, canShowBlockScreens = canOverlay)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppLimitsUiState(),
    )

    init {
        refresh()
    }

    /**
     * Re-read usage from the system. Called on every screen resume, because usage accrues
     * while the user is inside the app they limited — a snapshot taken once at construction
     * is stale by the time they come back to look at it.
     */
    fun refresh() {
        usageAccessGranted.value = appLimitRepository.hasUsageAccess()
        canShowBlockScreens.value = PlatformGuardPolicy.canDrawOverlays(applicationContext)
        viewModelScope.launch {
            appLimitRepository.refreshUsageSnapshot()
            usageAccessGranted.value = appLimitRepository.hasUsageAccess()
        }
    }

    /**
     * Assemble each group from its explicitly-assigned apps, falling back to category
     * matching for groups the user has never edited. Apps assigned to another group are
     * excluded from the fallback so they never appear in two groups at once.
     */
    private fun buildGroups(
        rows: List<AppLimitRowUiState>,
        assignmentByPackage: Map<String, String>,
    ): List<AppLimitGroupUiState> = GroupDefinitions.map { definition ->
        val assigned = rows.filter { assignmentByPackage[it.app.packageName] == definition.id }
        val members = assigned.ifEmpty {
            rows.filter { row ->
                row.app.category in definition.categories &&
                    assignmentByPackage[row.app.packageName] == null
            }
        }

        // The shared timer is whatever the group's enabled rules agree on; if none are
        // enabled, fall back to a configured-but-off rule so editing shows the saved value.
        val activeRule = members.firstOrNull { it.rule?.enabled == true }?.rule
        val anyRule = activeRule ?: members.firstOrNull { it.rule != null }?.rule

        AppLimitGroupUiState(
            id = definition.id,
            title = definition.title,
            apps = members,
            limitMinutes = anyRule?.dailyLimitMinutes,
            limitEnabled = activeRule != null,
            usedMinutes = members.sumOf { it.usedMinutes },
            suggestedLimitMinutes = definition.suggestedLimitMinutes,
        )
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

    /**
     * Save a group's membership and its single shared timer. Apps removed from the group
     * lose their rule outright — otherwise unchecking an app would leave it silently limited
     * by a group it no longer belongs to.
     */
    fun saveGroupLimit(groupId: String, packageNames: Set<String>, enabled: Boolean, limitMinutes: Int) {
        viewModelScope.launch {
            val previous = appLimitRepository.packagesInGroup(groupId)
            appLimitRepository.saveGroupAssignments(groupId, packageNames)

            (previous - packageNames).forEach { removed ->
                appLimitRepository.deleteRule(removed)
                cancelEnforceWork(removed)
            }

            packageNames.forEach { packageName ->
                appLimitRepository.saveRule(
                    AppLimitRule(
                        packageName = packageName,
                        enabled = enabled,
                        dailyLimitMinutes = limitMinutes,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
                if (enabled) scheduleEnforceForPreset(packageName, limitMinutes) else cancelEnforceWork(packageName)
            }
            appLimitRepository.refreshUsageSnapshot()
        }
    }

    /** Turn a whole group's shared timer on or off without touching its membership. */
    fun setGroupEnabled(group: AppLimitGroupUiState, enabled: Boolean) {
        val limit = group.limitMinutes ?: group.suggestedLimitMinutes
        saveGroupLimit(group.id, group.apps.map { it.app.packageName }.toSet(), enabled, limit)
    }

    /** Remove a group's shared timer entirely, leaving the apps unlimited. */
    fun clearGroupLimit(group: AppLimitGroupUiState) {
        viewModelScope.launch {
            group.apps.forEach { row ->
                appLimitRepository.deleteRule(row.app.packageName)
                cancelEnforceWork(row.app.packageName)
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

    /**
     * Spend an extension from the App Limits screen.
     *
     * Enforcement work is only (re)scheduled when the grant actually succeeded. Rescheduling on a
     * refusal would push the enforcement worker out past a block that is supposed to be in force,
     * which is how a denied extension could still buy time.
     */
    fun grantOverride(packageName: String, minutes: Int) {
        viewModelScope.launch {
            val result = appLimitRepository.extendOverride(packageName, minutes)
            appLimitRepository.refreshUsageSnapshot()
            when (result) {
                is OverrideResult.Granted -> {
                    val delay = (result.untilEpochMs - System.currentTimeMillis())
                        .coerceAtLeast(0L)
                    scheduleEnforceWork(packageName, delay)
                }

                is OverrideResult.Denied -> _overrideDenied.emit(result.reason)
            }
        }
    }

    /** Change the daily extension budget. See [AppLimitRepository.setExtensionCaps]. */
    fun setExtensionCaps(extensionsPerDay: Int, extraMinutesPerDay: Int) {
        viewModelScope.launch {
            val immediate = appLimitRepository.setExtensionCaps(extensionsPerDay, extraMinutesPerDay)
            if (!immediate) _capChangeDeferred.emit(Unit)
            refreshExtensionCaps()
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
            appLimitRepository.scheduleApproachAlarms(packageName)
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

    private companion object {
        /** Mirrors the defaults in [com.calmlauncher.domain.model.LauncherSettings]. */
        val DefaultCaps = AppLimitExtensionCaps(extensionsPerDay = 2, extraMinutesPerDay = 20)
    }
}
