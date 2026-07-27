package com.calmlauncher.di

import android.content.Context
import androidx.room.Room
import com.calmlauncher.data.db.AnalyticsDao
import com.calmlauncher.data.db.AppMetaDao
import com.calmlauncher.data.db.AppLimitDao
import com.calmlauncher.data.db.CALM_MIGRATIONS
import com.calmlauncher.data.db.CalmDatabase
import com.calmlauncher.data.db.LaunchEventDao
import com.calmlauncher.data.db.ReflectionDao
import com.calmlauncher.data.db.ReminderDao
import com.calmlauncher.data.db.RiskStateDao
import com.calmlauncher.data.db.ScreenTimeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): CalmDatabase = Room.databaseBuilder(
        context,
        CalmDatabase::class.java,
        CalmDatabase.NAME,
    )
        // Real migrations first, so an additive schema change keeps the user's history.
        // The destructive fallback stays as a last resort for versions with no path.
        .addMigrations(*CALM_MIGRATIONS)
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideAppMetaDao(db: CalmDatabase): AppMetaDao = db.appMetaDao()

    @Provides
    fun provideAnalyticsDao(db: CalmDatabase): AnalyticsDao = db.analyticsDao()

    @Provides
    fun provideAppLimitDao(db: CalmDatabase): AppLimitDao = db.appLimitDao()

    @Provides
    fun provideLaunchEventDao(db: CalmDatabase): LaunchEventDao = db.launchEventDao()

    @Provides
    fun provideReflectionDao(db: CalmDatabase): ReflectionDao = db.reflectionDao()

    @Provides
    fun provideReminderDao(db: CalmDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideScreenTimeDao(db: CalmDatabase): ScreenTimeDao = db.screenTimeDao()

    @Provides
    fun provideRiskStateDao(db: CalmDatabase): RiskStateDao = db.riskStateDao()
}
