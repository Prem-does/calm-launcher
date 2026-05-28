package com.calmlauncher.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calmlauncher.data.db.CalmDatabaseProvider
import com.calmlauncher.data.db.entity.LaunchEventEntity
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import com.calmlauncher.data.db.entity.SettingsEntity
import com.calmlauncher.data.repository.LaunchEventRepository
import com.calmlauncher.data.repository.LaunchEventRepositoryImpl
import com.calmlauncher.data.repository.ScreenTimeRepository
import com.calmlauncher.data.repository.ScreenTimeRepositoryImpl
import com.calmlauncher.data.repository.SettingsRepository
import com.calmlauncher.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class LauncherSettingsState(
    val pinHash: String? = null,
    val pinProtected: Boolean = false,
    val grayscaleForced: Boolean = true,
    val kioskModeEnabled: Boolean = false,
    val hiddenStatusBar: Boolean = false,
    val focusModeEnabled: Boolean = false,
    val settingsUnlocked: Boolean = false,
    val preferences: Map<String, String> = emptyMap()
)

data class LauncherUiState(
    val settings: LauncherSettingsState = LauncherSettingsState(),
    val todayScreenTimeMinutes: Int = 0,
    val isForegroundSessionRunning: Boolean = false,
    val recentApps: List<String> = emptyList(),
    val launchAttempts: Map<String, Int> = emptyMap(),
    val unlockCount: Int = 0,
    val dopamineRiskMessage: String = "",
    val calmInsight: String = ""
)

class LauncherStateViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CalmDatabaseProvider.get(application)
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database.settingsDao())
    private val screenTimeRepository: ScreenTimeRepository = ScreenTimeRepositoryImpl(database.screenTimeDao())
    private val launchEventRepository: LaunchEventRepository = LaunchEventRepositoryImpl(database.launchEventDao())

    private val dayKey: String
        get() = LocalDate.now().toString()

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private var sessionStartMillis: Long? = null
    private var lastFocusModeEnabledAtMillis: Long = 0L
    private val foregroundStarts = ArrayDeque<Long>()
    private val launchHistory = ArrayDeque<Pair<String, Long>>()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { entity ->
                _uiState.value = _uiState.value.copy(
                    settings = entity.toState(_uiState.value.settings)
                )
            }
        }

        viewModelScope.launch {
            screenTimeRepository.observeDay(dayKey).collect { entity ->
                _uiState.value = _uiState.value.copy(todayScreenTimeMinutes = entity?.minutesUsed ?: 0)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (screenTimeRepository.getDay(dayKey) == null) {
                screenTimeRepository.setDayTotal(dayKey, 0)
            }
            if (settingsRepository.observeSettings().firstOrNull() == null) {
                settingsRepository.saveSettings(LauncherSettingsState().toEntity())
            }
        }

        viewModelScope.launch {
            launchEventRepository.observeRecentLaunches().collect { events ->
                _uiState.value = _uiState.value.copy(calmInsight = buildCalmInsight(events))
            }
        }
    }

    fun startForegroundSession() {
        if (sessionStartMillis == null) {
            sessionStartMillis = System.currentTimeMillis()
            foregroundStarts.addLast(sessionStartMillis ?: return)
            trimOlderThan(foregroundStarts, System.currentTimeMillis() - 60 * 60 * 1000L)
            _uiState.value = _uiState.value.copy(
                isForegroundSessionRunning = true,
                unlockCount = _uiState.value.unlockCount + 1
            )
        }
    }

    fun endForegroundSession() {
        val startedAt = sessionStartMillis ?: return
        val minutes = ((System.currentTimeMillis() - startedAt) / 60000L).toInt().coerceAtLeast(0)
        sessionStartMillis = null
        _uiState.value = _uiState.value.copy(isForegroundSessionRunning = false)
        viewModelScope.launch(Dispatchers.IO) {
            val current = screenTimeRepository.getDay(dayKey)
            val newTotal = (current?.minutesUsed ?: 0) + minutes.coerceAtLeast(1)
            screenTimeRepository.setDayTotal(dayKey, newTotal)
        }
    }

    fun setPin(pin: String) {
        updateSettings { current -> current.copy(pinHash = pin.sha256(), pinProtected = true, settingsUnlocked = true) }
    }

    fun unlockSettings(pin: String) {
        val current = _uiState.value.settings
        if (current.pinHash == null || current.pinHash == pin.sha256()) {
            updateSettings { it.copy(settingsUnlocked = true) }
        }
    }

    fun lockSettings() {
        updateSettings { it.copy(settingsUnlocked = false) }
    }

    fun toggleGrayscale() {
        updateSettings { it.copy(grayscaleForced = !it.grayscaleForced) }
    }

    fun toggleKioskMode() {
        updateSettings { it.copy(kioskModeEnabled = !it.kioskModeEnabled) }
    }

    fun toggleHiddenStatusBar() {
        updateSettings { it.copy(hiddenStatusBar = !it.hiddenStatusBar) }
    }

    fun toggleFocusMode() {
        val current = _uiState.value.settings
        val now = System.currentTimeMillis()
        if (current.focusModeEnabled && current.lockDeviceDuringFocusSessions()) {
            val lockedUntil = lastFocusModeEnabledAtMillis + FocusExitCooldownMillis
            if (now < lockedUntil) return
        }
        updateSettings {
            val enabled = !it.focusModeEnabled
            if (enabled) lastFocusModeEnabledAtMillis = now
            it.copy(focusModeEnabled = enabled)
        }
    }

    fun emergencyBypassFocusMode() {
        updateSettings { it.copy(focusModeEnabled = false) }
    }

    fun setPreference(key: String, value: String) {
        updateSettings { current -> current.copy(preferences = current.preferences + (key to value)) }
    }

    fun togglePreference(key: String, onLabel: String = "ON", offLabel: String = "OFF") {
        val currentValue = _uiState.value.settings.preferences[key]
        val nextValue = if (currentValue == onLabel) offLabel else onLabel
        setPreference(key, nextValue)
    }

    fun saveScreenTime(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = screenTimeRepository.getDay(dayKey)
            val newTotal = (current?.minutesUsed ?: 0) + minutes.coerceAtLeast(1)
            screenTimeRepository.setDayTotal(dayKey, newTotal)
        }
    }

    fun recordAppLaunch(label: String): Boolean {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return false
        val now = System.currentTimeMillis()
        val nextCount = (_uiState.value.launchAttempts[trimmedLabel] ?: 0) + 1
        val riskMessage = detectDopamineRisk(trimmedLabel, now)
        val shouldHideFromHome = _uiState.value.settings.shouldHideFromHomeSurface(trimmedLabel)
        val updatedRecentApps = if (shouldHideFromHome) {
            _uiState.value.recentApps
        } else {
            listOf(trimmedLabel) + _uiState.value.recentApps
                .filterNot { it.equals(trimmedLabel, ignoreCase = true) }
                .take(4)
        }
        _uiState.value = _uiState.value.copy(
            recentApps = updatedRecentApps,
            launchAttempts = _uiState.value.launchAttempts + (trimmedLabel to nextCount),
            dopamineRiskMessage = riskMessage
        )
        if (riskMessage.isNotBlank() && _uiState.value.settings.dopamineDetectionEngine()) {
            applyDopamineFriction()
        }
        return _uiState.value.settings.shouldDeadEndFeedFor(trimmedLabel) && (nextCount >= 2 || riskMessage.isNotBlank())
    }

    fun recordLaunchReason(packageName: String, label: String, reason: String) {
        if (packageName.isBlank() || label.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            launchEventRepository.recordLaunchReason(packageName, label, reason)
        }
    }

    private fun updateSettings(transform: (LauncherSettingsState) -> LauncherSettingsState) {
        val updated = transform(_uiState.value.settings)
        _uiState.value = _uiState.value.copy(settings = updated)
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.saveSettings(updated.toEntity())
        }
    }

    private fun detectDopamineRisk(label: String, now: Long): String {
        launchHistory.addLast(label to now)
        while (launchHistory.isNotEmpty() && launchHistory.first().second < now - 60 * 60 * 1000L) {
            launchHistory.removeFirst()
        }
        trimOlderThan(foregroundStarts, now - 60 * 60 * 1000L)

        val recentLaunches = launchHistory.filter { it.second >= now - 10 * 60 * 1000L }
        val sameAppLoops = recentLaunches.count { it.first.equals(label, ignoreCase = true) }
        val rapidSwitches = recentLaunches.zipWithNext().count { (first, second) ->
            !first.first.equals(second.first, ignoreCase = true) && second.second - first.second <= 45 * 1000L
        }
        val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
        val lateNightSpike = (hour >= 23 || hour < 5) && recentLaunches.size >= 3
        val repeatedUnlocks = foregroundStarts.size >= 8

        return when {
            sameAppLoops >= 3 && lateNightSpike -> "You are reopening $label repeatedly late at night."
            sameAppLoops >= 3 -> "You are reopening $label every few minutes."
            rapidSwitches >= 4 -> "You are switching apps quickly."
            repeatedUnlocks -> "You have unlocked your phone often this hour."
            lateNightSpike -> "Your late-night usage is rising."
            else -> ""
        }
    }

    private fun applyDopamineFriction() {
        val current = _uiState.value.settings
        val shouldEnterFocus = current.recoveryMode() || current.dynamicMinimalism()
        if (current.grayscaleForced && (!shouldEnterFocus || current.focusModeEnabled)) return
        updateSettings {
            it.copy(
                grayscaleForced = true,
                focusModeEnabled = it.focusModeEnabled || shouldEnterFocus
            )
        }
    }
}

private fun trimOlderThan(values: ArrayDeque<Long>, cutoffMillis: Long) {
    while (values.isNotEmpty() && values.first() < cutoffMillis) {
        values.removeFirst()
    }
}

private const val FocusExitCooldownMillis = 10 * 60 * 1000L

private fun SettingsEntity?.toState(fallback: LauncherSettingsState): LauncherSettingsState {
    if (this == null) return fallback
    return LauncherSettingsState(
        pinHash = pinHash,
        pinProtected = pinProtected,
        grayscaleForced = grayscaleForced,
        kioskModeEnabled = kioskModeEnabled,
        hiddenStatusBar = hiddenStatusBar,
        focusModeEnabled = focusModeEnabled,
        settingsUnlocked = fallback.settingsUnlocked,
        preferences = parsePreferencesBlob(preferencesBlob)
    )
}

private fun LauncherSettingsState.toEntity(): SettingsEntity {
    return SettingsEntity(
        id = 0,
        pinHash = pinHash,
        pinProtected = pinProtected,
        grayscaleForced = grayscaleForced,
        kioskModeEnabled = kioskModeEnabled,
        hiddenStatusBar = hiddenStatusBar,
        focusModeEnabled = focusModeEnabled,
        preferencesBlob = buildPreferencesBlob()
    )
}

private fun LauncherSettingsState.buildPreferencesBlob(): String {
    return preferences.entries.joinToString("\n") { (key, value) -> "$key=$value" }
}

private fun parsePreferencesBlob(blob: String): Map<String, String> {
    if (blob.isBlank()) return emptyMap()
    return blob.lineSequence().mapNotNull { line ->
        val index = line.indexOf('=')
        if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
    }.toMap()
}

private fun buildCalmInsight(events: List<LaunchEventEntity>): String {
    if (events.isEmpty()) return "No launch journal yet."

    val labelCounts = events.groupingBy { it.label.lowercase() }.eachCount()
    val reasonCounts = events.mapNotNull { event ->
        val reason = event.reason.trim().lowercase()
        if (reason.isBlank()) null else reason
    }.groupingBy { it }.eachCount()
    val topLabel = labelCounts.maxByOrNull { it.value }?.key?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val topReason = reasonCounts.maxByOrNull { it.value }?.key
    val nightCount = events.count {
        val hour = Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).hour
        hour >= 21 || hour < 6
    }

    return when {
        topLabel != null && topReason != null && nightCount > events.size / 2 -> "You open $topLabel most when $topReason, usually at night."
        topLabel != null && topReason != null -> "You open $topLabel most when $topReason."
        topLabel != null -> "You open $topLabel most often."
        else -> "Your phone usage is still forming a pattern."
    }
}

private fun String.sha256(): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(toByteArray()).joinToString("") { "%02x".format(it) }
}
