package com.calmlauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calmlauncher.data.db.entity.AppUsageEntity
import com.calmlauncher.data.db.entity.AppMetaEntity
import com.calmlauncher.data.db.entity.AppLimitEventEntity
import com.calmlauncher.data.db.entity.AppLimitGroupAssignmentEntity
import com.calmlauncher.data.db.entity.AppLimitRuleEntity
import com.calmlauncher.data.db.entity.AppLimitUsageEntity
import com.calmlauncher.data.db.entity.DailyUsageEntity
import com.calmlauncher.data.db.entity.LaunchEventEntity
import com.calmlauncher.data.db.entity.NotificationEventEntity
import com.calmlauncher.data.db.entity.ReflectionEntity
import com.calmlauncher.data.db.entity.RiskStateEntity
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import com.calmlauncher.data.db.entity.SessionEntity
import com.calmlauncher.data.db.entity.UnlockEntity

/**
 * Room database for all persisted launcher state. Schema export is disabled (single
 * shipping version); destructive migration is configured in [com.calmlauncher.di.DatabaseModule]
 * since the data here is derived/recoverable.
 */
@Database(
    entities = [
        AppUsageEntity::class,
        AppMetaEntity::class,
        AppLimitEventEntity::class,
        AppLimitGroupAssignmentEntity::class,
        AppLimitRuleEntity::class,
        AppLimitUsageEntity::class,
        DailyUsageEntity::class,
        LaunchEventEntity::class,
        NotificationEventEntity::class,
        ReflectionEntity::class,
        ScreenTimeEntity::class,
        SessionEntity::class,
        UnlockEntity::class,
        RiskStateEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class CalmDatabase : RoomDatabase() {
    abstract fun analyticsDao(): AnalyticsDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun appLimitDao(): AppLimitDao
    abstract fun launchEventDao(): LaunchEventDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun riskStateDao(): RiskStateDao

    companion object {
        const val NAME = "calm.db"
    }
}
