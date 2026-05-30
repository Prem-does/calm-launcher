package com.calmlauncher.domain.usecase

import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.LauncherSettings
import com.calmlauncher.domain.policy.EnvironmentRules
import com.calmlauncher.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Applies the *behavioural* visibility rules on top of the catalog's own hidden-app handling:
 *
 *  - **Invisible Social Apps** ([LauncherSettings.hideSocialApps]) — drops SOCIAL apps from the
 *    default list so they're only reachable by deliberate search/intent.
 *  - **Environment visibility** — hides whatever the active [com.calmlauncher.domain.model.EnvironmentMode]
 *    currently blocks (e.g. Sleep hides entertainment/browsers), keeping the list honest about
 *    what you can actually open right now.
 *
 * Favourites are never filtered out here: pinning an app is an explicit, deliberate choice, so
 * it stays reachable from the home screen even when its category is otherwise suppressed.
 *
 * The pure [filter] is the core; [invoke] simply lifts it over a settings flow.
 */
class FilterAppsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    /** Pure filter: given a snapshot of [apps] and [settings], return the visible subset. */
    fun filter(apps: List<AppEntry>, settings: LauncherSettings): List<AppEntry> =
        apps.filter { app -> isVisible(app, settings) }

    private fun isVisible(app: AppEntry, settings: LauncherSettings): Boolean {
        // Explicit favourites always remain reachable.
        if (app.isFavorite) return true

        // Invisible Social Apps: hide SOCIAL from the default list.
        if (settings.hideSocialApps && app.category == AppCategory.SOCIAL) {
            return false
        }

        // Environment visibility: hide categories the current environment blocks.
        if (EnvironmentRules.blocks(settings.environmentMode, app.category, settings.focusActive)) {
            return false
        }

        return true
    }

    /** Flow variant: combines an upstream app list with the live settings. */
    operator fun invoke(apps: Flow<List<AppEntry>>): Flow<List<AppEntry>> =
        combine(apps, settingsRepository.settings) { list, settings ->
            filter(list, settings)
        }
}
