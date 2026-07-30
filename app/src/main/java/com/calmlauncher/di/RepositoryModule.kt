package com.calmlauncher.di

import com.calmlauncher.data.repository.AppRepositoryImpl
import com.calmlauncher.data.repository.AnalyticsRepositoryImpl
import com.calmlauncher.data.repository.AppLimitRepositoryImpl
import com.calmlauncher.data.repository.AppearanceRepositoryImpl
import com.calmlauncher.data.repository.LaunchEventRepositoryImpl
import com.calmlauncher.data.repository.ReflectionRepositoryImpl
import com.calmlauncher.data.repository.ReminderRepositoryImpl
import com.calmlauncher.data.repository.RiskRepositoryImpl
import com.calmlauncher.data.repository.ScreenTimeRepositoryImpl
import com.calmlauncher.data.repository.SettingsRepositoryImpl
import com.calmlauncher.data.system.AppLauncherImpl
import com.calmlauncher.data.system.SystemActionsImpl
import com.calmlauncher.domain.repository.AppRepository
import com.calmlauncher.domain.repository.AnalyticsRepository
import com.calmlauncher.domain.repository.AppLimitRepository
import com.calmlauncher.domain.repository.AppearanceRepository
import com.calmlauncher.domain.repository.LaunchEventRepository
import com.calmlauncher.domain.repository.ReflectionRepository
import com.calmlauncher.domain.repository.ReminderRepository
import com.calmlauncher.domain.repository.RiskRepository
import com.calmlauncher.domain.repository.ScreenTimeRepository
import com.calmlauncher.domain.repository.SettingsRepository
import com.calmlauncher.domain.service.AppLauncher
import com.calmlauncher.domain.service.SystemActions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain interfaces to their data-layer implementations. Scopes are declared on the
 * @Binds methods so the impl classes themselves stay annotation-free (just @Inject ctors).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindAppearanceRepository(impl: AppearanceRepositoryImpl): AppearanceRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindAppLimitRepository(impl: AppLimitRepositoryImpl): AppLimitRepository

    @Binds
    @Singleton
    abstract fun bindScreenTimeRepository(impl: ScreenTimeRepositoryImpl): ScreenTimeRepository

    @Binds
    @Singleton
    abstract fun bindLaunchEventRepository(impl: LaunchEventRepositoryImpl): LaunchEventRepository

    @Binds
    @Singleton
    abstract fun bindReflectionRepository(impl: ReflectionRepositoryImpl): ReflectionRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindRiskRepository(impl: RiskRepositoryImpl): RiskRepository

    @Binds
    @Singleton
    abstract fun bindAppLauncher(impl: AppLauncherImpl): AppLauncher

    @Binds
    @Singleton
    abstract fun bindSystemActions(impl: SystemActionsImpl): SystemActions
}
