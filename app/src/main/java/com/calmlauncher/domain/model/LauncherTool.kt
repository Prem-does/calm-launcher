package com.calmlauncher.domain.model

/**
 * The built-in tools the launcher exposes by default (README §App System). Each is
 * resolved to a concrete on-device package by the catalog, preferring Samsung One UI
 * apps and falling back to Google/AOSP equivalents.
 */
enum class LauncherTool(val displayName: String, val category: AppCategory) {
    PHONE("Phone", AppCategory.COMMUNICATION),
    MESSAGES("Messages", AppCategory.COMMUNICATION),
    ALARM("Alarm", AppCategory.TOOL),
    CALCULATOR("Calculator", AppCategory.TOOL),
    CALENDAR("Calendar", AppCategory.TOOL),
    CAMERA("Camera", AppCategory.TOOL),
    MAPS("Maps", AppCategory.TOOL),
    NOTES("Notes", AppCategory.TOOL),
    MUSIC("Music", AppCategory.TOOL),
    SETTINGS("Settings", AppCategory.TOOL),
}
