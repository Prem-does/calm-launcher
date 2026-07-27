package com.calmlauncher.navigation

/**
 * Top-level navigation destinations. The launch gate and the dead-end reset are shown
 * as overlays driven by the launch coordinator rather than nav routes, so they can sit
 * above any screen during the friction sequence.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val APPS = "apps"
    const val SEARCH = "search"
    const val FOCUS = "focus"
    const val REFLECTION = "reflection"
    const val RESET = "reset"

    const val SETTINGS = "settings"
    const val SETTINGS_MANAGE_APPS = "settings/apps"
    const val SETTINGS_SCREEN_TIME = "settings/screen_time"
    const val SETTINGS_APP_LIMITS = "settings/app_limits"
    const val SETTINGS_FRICTION = "settings/friction"
    const val SETTINGS_ENVIRONMENT = "settings/environment"
    const val SETTINGS_REMINDERS = "settings/reminders"

    /** The three primary bottom-nav tabs, in order. */
    val bottomTabs = listOf(HOME, APPS, FOCUS)
}
