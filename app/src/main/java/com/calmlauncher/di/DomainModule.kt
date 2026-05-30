package com.calmlauncher.di

import com.calmlauncher.domain.policy.DefaultModeEngine
import com.calmlauncher.domain.policy.ModeEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the pure domain policy engine. Use cases and [com.calmlauncher.domain.policy.RiskEvaluator]
 * are `@Inject constructor`, so they need no explicit bindings here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    abstract fun bindModeEngine(impl: DefaultModeEngine): ModeEngine
}
