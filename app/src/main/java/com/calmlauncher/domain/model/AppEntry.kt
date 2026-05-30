package com.calmlauncher.domain.model

/**
 * Broad behavioural category for an installed app. Drives default friction and
 * visibility decisions (social/entertainment/browser/store are "distracting by
 * default"). The user can override an app's category in Manage Apps.
 */
enum class AppCategory {
    TOOL,
    COMMUNICATION,
    SOCIAL,
    ENTERTAINMENT,
    BROWSER,
    STORE,
    GAME,
    OTHER;

    val isDistractingByDefault: Boolean
        get() = this == SOCIAL || this == ENTERTAINMENT || this == BROWSER ||
            this == STORE || this == GAME
}

/**
 * A launchable application as the launcher understands it. Icons are intentionally
 * NOT held here — they are resolved lazily in the UI layer (keyed by [packageName])
 * so the domain stays free of Android drawables. [isFavorite] is derived from
 * settings; [isHidden]/[isDistracting]/[category] are persisted per app.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val category: AppCategory = AppCategory.OTHER,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isDistracting: Boolean = category.isDistractingByDefault,
    val isSystem: Boolean = false,
)
