package com.calmlauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calmlauncher.data.db.entity.AppEntity
import com.calmlauncher.data.db.entity.LaunchEventEntity
import com.calmlauncher.data.db.entity.ScreenTimeEntity
import com.calmlauncher.data.db.entity.SettingsEntity
import com.calmlauncher.data.db.entity.TelemetryEvent

@Database(
    entities = [AppEntity::class, LaunchEventEntity::class, SettingsEntity::class, ScreenTimeEntity::class, TelemetryEvent::class],
    version = 4,
    exportSchema = false
)
abstract class CalmDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun screenTimeDao(): ScreenTimeDao
    abstract fun launchEventDao(): LaunchEventDao
    abstract fun telemetryDao(): TelemetryDao
}

