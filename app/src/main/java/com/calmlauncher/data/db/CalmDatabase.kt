package com.calmlauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calmlauncher.data.db.entity.AppMetaEntity
import com.calmlauncher.data.db.entity.AppLimitEventEntity
import com.calmlauncher.data.db.entity.AppLimitRuleEntity
import com.calmlauncher.data.db.entity.AppLimitUsageEntity
import com.calmlauncher.data.db.entity.LaunchEventEntity
import com.calmlauncher.data.db.entity.ReflectionEntity
import com.calmlauncher.data.db.entity.RiskStateEntity
import com.calmlauncher.data.db.entity.ScreenTimeEntity

/**
 * Room database for all persisted launcher state. Schema export is disabled (single
 * shipping version); destructive migration is configured in [com.calmlauncher.di.DatabaseModule]
 * since the data here is derived/recoverable.
 */
@Database(
    entities = [
        AppMetaEntity::class,
        AppLimitEventEntity::class,
        AppLimitRuleEntity::class,
        AppLimitUsageEntity::class,
        LaunchEventEntity::class,
        ReflectionEntity::class,
        ScreenTimeEntity::class,
        RiskStateEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class CalmDatabase : RoomDatabase() {
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
