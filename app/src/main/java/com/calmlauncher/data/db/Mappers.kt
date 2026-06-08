package com.calmlauncher.data.db

import com.calmlauncher.data.db.entity.AppMetaEntity
import com.calmlauncher.data.db.entity.AppLimitEventEntity
import com.calmlauncher.data.db.entity.AppLimitRuleEntity
import com.calmlauncher.data.db.entity.AppLimitUsageEntity
import com.calmlauncher.data.db.entity.LaunchEventEntity
import com.calmlauncher.data.db.entity.ReflectionEntity
import com.calmlauncher.data.db.entity.RiskStateEntity
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppLimitDecision
import com.calmlauncher.domain.model.AppLimitEvent
import com.calmlauncher.domain.model.AppLimitEventType
import com.calmlauncher.domain.model.AppLimitRule
import com.calmlauncher.domain.model.AppLimitStatus
import com.calmlauncher.domain.model.AppLimitSummary
import com.calmlauncher.domain.model.AppLimitUsage
import com.calmlauncher.domain.model.LaunchEvent
import com.calmlauncher.domain.model.LaunchSource
import com.calmlauncher.domain.model.ReflectionEntry
import com.calmlauncher.domain.model.RiskState
import com.calmlauncher.domain.model.RiskTier
import com.calmlauncher.domain.model.ScreenTimeRecord
import org.json.JSONObject

// ---------------------------------------------------------------------------
// Safe enum parsing helpers — persisted strings are validated on the way out so
// a bad/renamed value never crashes the launcher.
// ---------------------------------------------------------------------------

internal fun String?.toAppCategory(): AppCategory =
    AppCategory.entries.firstOrNull { it.name == this } ?: AppCategory.OTHER

internal fun String?.toRiskTier(): RiskTier =
    RiskTier.entries.firstOrNull { it.name == this } ?: RiskTier.CALM

internal fun String?.toLaunchSource(): LaunchSource =
    LaunchSource.entries.firstOrNull { it.name == this } ?: LaunchSource.APP_LIST

// ---------------------------------------------------------------------------
// AppMetaEntity — note there is no full domain twin; AppEntry is assembled from
// the live PackageManager catalog combined with this metadata in the repository.
// ---------------------------------------------------------------------------

/** Resolved "is distracting" for this app: explicit override or the category default. */
internal fun AppMetaEntity.resolveDistracting(): Boolean =
    isDistractingOverride ?: category.toAppCategory().isDistractingByDefault

// ---------------------------------------------------------------------------
// LaunchEvent
// ---------------------------------------------------------------------------

internal fun LaunchEventEntity.toDomain(): LaunchEvent = LaunchEvent(
    id = id,
    packageName = packageName,
    category = category.toAppCategory(),
    timestampEpochMs = timestampEpochMs,
    reason = reason,
    source = source.toLaunchSource(),
)

internal fun LaunchEvent.toEntity(): LaunchEventEntity = LaunchEventEntity(
    id = id,
    packageName = packageName,
    category = category.name,
    timestampEpochMs = timestampEpochMs,
    reason = reason,
    source = source.name,
)

// ---------------------------------------------------------------------------
// ReflectionEntry
// ---------------------------------------------------------------------------

internal fun ReflectionEntity.toDomain(): ReflectionEntry = ReflectionEntry(
    id = dayStartEpochMs,
    dayStartEpochMs = dayStartEpochMs,
    prompt = prompt,
    response = response,
    createdAtEpochMs = createdAtEpochMs,
)

internal fun ReflectionEntry.toEntity(): ReflectionEntity = ReflectionEntity(
    dayStartEpochMs = dayStartEpochMs,
    prompt = prompt,
    response = response,
    createdAtEpochMs = createdAtEpochMs,
)

// ---------------------------------------------------------------------------
// ScreenTimeRecord — perApp map persisted as a JSON object string.
// ---------------------------------------------------------------------------

internal fun encodePerApp(perApp: Map<String, Long>): String {
    val json = JSONObject()
    for ((pkg, ms) in perApp) {
        json.put(pkg, ms)
    }
    return json.toString()
}

internal fun decodePerApp(raw: String?): Map<String, Long> {
    if (raw.isNullOrBlank()) return emptyMap()
    return try {
        val json = JSONObject(raw)
        buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, json.optLong(key, 0L))
            }
        }
    } catch (_: Throwable) {
        emptyMap()
    }
}

internal fun ScreenTimeEntity.toDomain(): ScreenTimeRecord = ScreenTimeRecord(
    dayStartEpochMs = dayStartEpochMs,
    totalForegroundMs = totalForegroundMs,
    perApp = decodePerApp(perAppJson),
)

internal fun ScreenTimeRecord.toEntity(): ScreenTimeEntity = ScreenTimeEntity(
    dayStartEpochMs = dayStartEpochMs,
    totalForegroundMs = totalForegroundMs,
    perAppJson = encodePerApp(perApp),
)

// ---------------------------------------------------------------------------
// App limit rule / usage / event
// ---------------------------------------------------------------------------

internal fun AppLimitRuleEntity.toDomain(): AppLimitRule = AppLimitRule(
    packageName = packageName,
    enabled = enabled,
    dailyLimitMinutes = dailyLimitMinutes,
    overrideUntilEpochMs = overrideUntilEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    lastNotifiedEpochMs = lastNotifiedEpochMs,
)

internal fun AppLimitRule.toEntity(): AppLimitRuleEntity = AppLimitRuleEntity(
    packageName = packageName,
    enabled = enabled,
    dailyLimitMinutes = dailyLimitMinutes,
    overrideUntilEpochMs = overrideUntilEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    lastNotifiedEpochMs = lastNotifiedEpochMs,
)

internal fun AppLimitUsageEntity.toDomain(): AppLimitUsage = AppLimitUsage(
    dayStartEpochMs = dayStartEpochMs,
    packageName = packageName,
    usedMs = usedMs,
    lastSyncedAtEpochMs = lastSyncedAtEpochMs,
)

internal fun AppLimitUsage.toEntity(): AppLimitUsageEntity = AppLimitUsageEntity(
    dayStartEpochMs = dayStartEpochMs,
    packageName = packageName,
    usedMs = usedMs,
    lastSyncedAtEpochMs = lastSyncedAtEpochMs,
)

internal fun AppLimitEventEntity.toDomain(): AppLimitEvent = AppLimitEvent(
    id = id,
    packageName = packageName,
    label = label,
    eventType = AppLimitEventType.entries.firstOrNull { it.name == eventType }
        ?: AppLimitEventType.BLOCKED,
    timestampEpochMs = timestampEpochMs,
    dayStartEpochMs = dayStartEpochMs,
    limitMinutes = limitMinutes,
    usedMinutes = usedMinutes,
    overrideMinutes = overrideMinutes,
)

internal fun AppLimitEvent.toEntity(): AppLimitEventEntity = AppLimitEventEntity(
    id = id,
    packageName = packageName,
    label = label,
    eventType = eventType.name,
    timestampEpochMs = timestampEpochMs,
    dayStartEpochMs = dayStartEpochMs,
    limitMinutes = limitMinutes,
    usedMinutes = usedMinutes,
    overrideMinutes = overrideMinutes,
)

internal fun AppLimitStatus.toSummary(blockedLaunchesToday: Int, topLimitedCount: Int): AppLimitSummary =
    AppLimitSummary(
        blockedLaunchesToday = blockedLaunchesToday,
        limitedAppsToday = if (blockedToday) 1 else 0,
        estimatedTimeSavedMinutes = blockedLaunchesToday * (dailyLimitMinutes ?: 0),
        topLimitedPackage = packageName,
        topLimitedCount = topLimitedCount,
    )

// ---------------------------------------------------------------------------
// RiskState
// ---------------------------------------------------------------------------

internal fun RiskStateEntity.toDomain(): RiskState = RiskState(
    tier = tier.toRiskTier(),
    score = score,
    repeatedOpens = repeatedOpens,
    rapidSwitches = rapidSwitches,
    lateNightLaunches = lateNightLaunches,
    longestSessionMs = longestSessionMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

internal fun RiskState.toEntity(): RiskStateEntity = RiskStateEntity(
    id = RiskStateEntity.SINGLETON_ID,
    tier = tier.name,
    score = score,
    repeatedOpens = repeatedOpens,
    rapidSwitches = rapidSwitches,
    lateNightLaunches = lateNightLaunches,
    longestSessionMs = longestSessionMs,
    updatedAtEpochMs = updatedAtEpochMs,
)
