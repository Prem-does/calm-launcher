package com.calmlauncher.data.system

import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import com.calmlauncher.domain.model.LauncherTool

/**
 * Known concrete package names for each [LauncherTool], ordered by preference:
 * Samsung One UI first, then Google, then AOSP. The catalog resolves a tool to the
 * first of these that is actually installed; if none match, [intentFallback] yields a
 * generic action intent (e.g. ACTION_DIAL) so the tool still works.
 */
object SamsungPackages {

    /** Preferred package candidates per tool, most-preferred first. */
    val candidates: Map<LauncherTool, List<String>> = mapOf(
        LauncherTool.PHONE to listOf(
            "com.samsung.android.dialer",
            "com.google.android.dialer",
            "com.android.dialer",
            "com.android.phone",
        ),
        LauncherTool.MESSAGES to listOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.messaging",
            "com.android.mms",
        ),
        LauncherTool.ALARM to listOf(
            "com.sec.android.app.clockpackage",
            "com.google.android.deskclock",
            "com.android.deskclock",
        ),
        LauncherTool.CALCULATOR to listOf(
            "com.sec.android.app.popupcalculator",
            "com.google.android.calculator",
            "com.android.calculator2",
        ),
        LauncherTool.CALENDAR to listOf(
            "com.samsung.android.calendar",
            "com.google.android.calendar",
            "com.android.calendar",
        ),
        LauncherTool.CAMERA to listOf(
            "com.sec.android.app.camera",
            "com.google.android.GoogleCamera",
            "com.android.camera2",
            "com.android.camera",
        ),
        LauncherTool.MAPS to listOf(
            "com.google.android.apps.maps",
        ),
        LauncherTool.NOTES to listOf(
            "com.samsung.android.app.notes",
            "com.google.android.keep",
        ),
        LauncherTool.MUSIC to listOf(
            "com.sec.android.app.music",
            "com.google.android.apps.youtube.music",
            "com.google.android.music",
        ),
        LauncherTool.SETTINGS to listOf(
            "com.android.settings",
        ),
    )

    fun candidatesFor(tool: LauncherTool): List<String> = candidates[tool].orEmpty()

    /**
     * A best-effort action intent for tools that can be invoked without a known package
     * (the system resolves a default handler). Returns null when there is no sensible
     * generic action (e.g. third-party-style tools like Maps/Notes/Music/Calculator).
     * Callers must add FLAG_ACTIVITY_NEW_TASK before starting.
     */
    fun intentFallback(tool: LauncherTool): Intent? = when (tool) {
        LauncherTool.PHONE -> Intent(Intent.ACTION_DIAL)
        LauncherTool.MESSAGES -> Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
        }
        LauncherTool.ALARM -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        LauncherTool.CALCULATOR -> Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALCULATOR)
        }
        LauncherTool.CALENDAR -> Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
        }
        LauncherTool.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        LauncherTool.MAPS -> Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MAPS)
        }
        LauncherTool.NOTES -> null
        LauncherTool.MUSIC -> Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MUSIC)
        }
        LauncherTool.SETTINGS -> Intent(android.provider.Settings.ACTION_SETTINGS)
    }
}
