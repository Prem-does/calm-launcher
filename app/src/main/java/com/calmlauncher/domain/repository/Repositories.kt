package com.calmlauncher.domain.repository

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.model.ReflectionEntry
import com.calmlauncher.domain.model.RiskState
import com.calmlauncher.domain.model.ScreenTimeRecord
import kotlinx.coroutines.flow.Flow

/**
 * The installed-app catalog plus per-app launcher metadata (hidden / favourite /
 * category). Backed by PackageManager + Room. [search] intentionally includes hidden
 * apps so social apps remain reachable by deliberate intent (Minimal Social Layer).
 */
interface AppRepository {
    fun observeApps(): Flow<List<AppEntry>>
    fun observeVisibleApps(): Flow<List<AppEntry>>
    fun observeFavorites(): Flow<List<AppEntry>>
    suspend fun getApp(packageName: String): AppEntry?
    suspend fun search(query: String): List<AppEntry>
    suspend fun refreshFromDevice()
    suspend fun setHidden(packageName: String, hidden: Boolean)
    suspend fun setFavorite(packageName: String, favorite: Boolean)
    suspend fun setFavoriteOrder(packageNames: List<String>)
    suspend fun setCategory(packageName: String, category: AppCategory)
}

/**
 * Single source of truth for user preferences (DataStore-backed). Mutations go
 * through [update] so callers compose copies, e.g. `update { it.copy(grayscaleEnabled = true) }`.
 */
interface SettingsRepository {
    val settings: Flow<LauncherSettings>
    suspend fun current(): LauncherSettings
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)
}

/** Daily foreground usage, sourced from UsageStatsManager when access is granted. */
interface ScreenTimeRepository {
    fun observeToday(): Flow<ScreenTimeRecord>
    suspend fun today(): ScreenTimeRecord
    fun observeRange(startEpochMs: Long, endEpochMs: Long): Flow<List<ScreenTimeRecord>>
    suspend fun refresh()
}

/** Append-only log of app opens with captured intent reasons. */
interface LaunchEventRepository {
    suspend fun record(event: LaunchEvent)
    fun observeSince(sinceEpochMs: Long): Flow<List<LaunchEvent>>
    suspend fun since(sinceEpochMs: Long): List<LaunchEvent>
    fun observeWeek(): Flow<List<LaunchEvent>>
}

/** Nightly reflection prompts + responses. */
interface ReflectionRepository {
    fun observeRecent(limit: Int = 14): Flow<List<ReflectionEntry>>
    suspend fun latestFor(dayStartEpochMs: Long): ReflectionEntry?
    suspend fun upsert(entry: ReflectionEntry)
}

/** Persisted output of the Dopamine Detection Engine. */
interface RiskRepository {
    val state: Flow<RiskState>
    suspend fun current(): RiskState
    suspend fun set(state: RiskState)
}
