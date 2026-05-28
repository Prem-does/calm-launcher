package com.calmlauncher.launcher

import com.calmlauncher.data.system.LauncherAppItem

private val essentialAppLabels = setOf(
    "Phone",
    "Messages",
    "Alarm",
    "Calculator",
    "Calendar",
    "Camera",
    "Maps",
    "Notes",
    "Settings"
)

private val entertainmentKeywords = listOf(
    "music",
    "video",
    "youtube",
    "netflix",
    "prime video",
    "spotify",
    "tiktok"
)

private val socialKeywords = listOf(
    "instagram",
    "twitter",
    "x",
    "facebook",
    "threads",
    "snapchat",
    "reddit"
)

private val addictiveKeywords = listOf(
    "browser",
    "chrome",
    "firefox",
    "youtube",
    "shorts",
    "tiktok",
    "instagram",
    "twitter",
    "facebook",
    "reddit",
    "snapchat",
    "music",
    "spotify",
    "video",
    "netflix",
    "prime video",
    "reels"
)

private val browserKeywords = listOf("browser", "chrome", "firefox", "samsung internet", "brave", "edge")
private val storeKeywords = listOf("play store", "store", "app store", "market")

// Master switch. When OFF (the default) the launcher behaves as a clean, non-restrictive
// minimal launcher: nothing is hidden, blocked, delayed, or locked. Turning "Enforcement" ON
// in Settings activates the signature restrictive behaviors below. Defaulting OFF keeps the
// device safe and fully reversible until the user explicitly opts in.
fun LauncherSettingsState.enforcementActive(): Boolean = prefEnabled("Enforcement", false)

fun LauncherSettingsState.prefEnabled(key: String, defaultValue: Boolean = false): Boolean {
    return preferences[key]?.let { value ->
        value.equals("ON", ignoreCase = true) || value.equals("true", ignoreCase = true)
    } ?: defaultValue
}

fun LauncherSettingsState.prefChoice(key: String, defaultValue: String): String {
    return preferences[key] ?: defaultValue
}

fun LauncherSettingsState.showTime(): Boolean = prefEnabled("Show Time", true)
fun LauncherSettingsState.showDate(): Boolean = prefEnabled("Show Date", true)
fun LauncherSettingsState.showBattery(): Boolean = prefEnabled("Show Battery %", true)
fun LauncherSettingsState.showWeather(): Boolean = prefEnabled("Show Weather", false)
fun LauncherSettingsState.showNextEvent(): Boolean = prefEnabled("Show Next Event", false)
fun LauncherSettingsState.centerAlignLayout(): Boolean = prefEnabled("Center Align Layout", true)
fun LauncherSettingsState.minimalClockStyle(): Boolean = prefEnabled("Minimal Clock Style", true)
fun LauncherSettingsState.largeClockStyle(): Boolean = prefEnabled("Large Clock Style", false)
fun LauncherSettingsState.hideSearchBar(): Boolean = prefEnabled("Hide Search Bar", false)
fun LauncherSettingsState.hideNavigationBar(): Boolean = prefEnabled("Hide Navigation Bar", false)
fun LauncherSettingsState.disableWidgets(): Boolean = prefEnabled("Disable Widgets", true)
fun LauncherSettingsState.allowedAppsListEnabled(): Boolean = if (!enforcementActive()) false else prefEnabled("Allowed Apps List", true)
fun LauncherSettingsState.hiddenAppsEnabled(): Boolean = prefEnabled("Hidden Apps", false)
fun LauncherSettingsState.lockAppsEnabled(): Boolean = if (!enforcementActive()) false else prefEnabled("Lock Apps", true)
fun LauncherSettingsState.focusModeAppsEnabled(): Boolean = prefEnabled("Focus Mode Apps", true)
fun LauncherSettingsState.essentialAppsOnly(): Boolean = prefEnabled("Essential Apps Only", true)
fun LauncherSettingsState.blockAppInstallation(): Boolean = if (!enforcementActive()) false else prefEnabled("Block App Installation", true)
fun LauncherSettingsState.disableAppSuggestions(): Boolean = if (!enforcementActive()) false else prefEnabled("Disable App Suggestions", true)
fun LauncherSettingsState.disableRecentlyUsedApps(): Boolean = if (!enforcementActive()) false else prefEnabled("Disable Recently Used Apps", true)
fun LauncherSettingsState.requireIntentBeforeOpening(): Boolean = if (!enforcementActive()) false else prefEnabled("Require Intent Before Opening", true)
fun LauncherSettingsState.intentBasedAppOpening(): Boolean = prefEnabled("Intent-Based App Opening", true)
fun LauncherSettingsState.requireLongPress(): Boolean = if (!enforcementActive()) false else prefEnabled("Require Long Press", true)
fun LauncherSettingsState.requirePinForApps(): Boolean = if (!enforcementActive()) false else prefEnabled("Require PIN For Apps", false)
fun LauncherSettingsState.dailyUsageLimitsEnabled(): Boolean = prefEnabled("Daily Usage Limits", false)
fun LauncherSettingsState.appCooldownTimerEnabled(): Boolean = prefEnabled("App Cooldown Timer", false)
fun LauncherSettingsState.appTimeoutAutoCloseEnabled(): Boolean = prefEnabled("App Timeout Auto-Close", false)
fun LauncherSettingsState.focusModeEnabledPref(): Boolean = if (!enforcementActive()) false else prefEnabled("Enable Focus Mode", true)
fun LauncherSettingsState.scheduleFocusMode(): Boolean = prefEnabled("Schedule Focus Mode", false)
fun LauncherSettingsState.emergencyBypassEnabled(): Boolean = prefEnabled("Emergency Bypass", true)
fun LauncherSettingsState.slowModeEnabled(): Boolean = prefEnabled("Slow Mode", false)
fun LauncherSettingsState.deadEndFeedsEnabled(): Boolean = prefEnabled("Dead-End Feeds", false)
fun LauncherSettingsState.dopamineDetectionEngine(): Boolean = prefEnabled("Dopamine Detection Engine", true)
fun LauncherSettingsState.oneAppAtATimeMode(): Boolean = prefEnabled("One-App-At-A-Time Mode", false)
fun LauncherSettingsState.analogMode(): Boolean = prefEnabled("Analog Mode", false)
fun LauncherSettingsState.dynamicMinimalism(): Boolean = prefEnabled("Dynamic Minimalism", false)
fun LauncherSettingsState.recoveryMode(): Boolean = prefEnabled("Recovery Mode", false)
fun LauncherSettingsState.eInkSimulation(): Boolean = prefEnabled("E-Ink Simulation", false)
fun LauncherSettingsState.breathUnlock(): Boolean = prefEnabled("Breath Unlock", false)
fun LauncherSettingsState.invisibleSocialApps(): Boolean = if (!enforcementActive()) false else prefEnabled("Invisible Social Apps", true)
fun LauncherSettingsState.minimalSocialLayer(): Boolean = if (!enforcementActive()) false else prefEnabled("Minimal Social Layer", true)
fun LauncherSettingsState.rewardRealLife(): Boolean = prefEnabled("Reward Real Life", true)
fun LauncherSettingsState.calmAIAssistant(): Boolean = prefEnabled("Calm AI Assistant", true)
fun LauncherSettingsState.usageReflectionScreen(): Boolean = prefEnabled("Usage Reflection Screen", true)
fun LauncherSettingsState.blockBrowser(): Boolean = if (!enforcementActive()) false else prefEnabled("Block Browser", true)
fun LauncherSettingsState.blockSocialMedia(): Boolean = if (!enforcementActive()) false else prefEnabled("Block Social Media", true)
fun LauncherSettingsState.blockPlayStore(): Boolean = if (!enforcementActive()) false else prefEnabled("Block Play Store", true)
fun LauncherSettingsState.hideNotifications(): Boolean = prefEnabled("Hide Notifications", true)
fun LauncherSettingsState.lockQuickSettings(): Boolean = prefEnabled("Lock Quick Settings", true)
fun LauncherSettingsState.ultraMinimalScreen(): Boolean = if (!enforcementActive()) false else prefEnabled("Ultra Minimal Screen", true)
fun LauncherSettingsState.allowCallsOnly(): Boolean = prefEnabled("Allow Calls Only", false)
fun LauncherSettingsState.allowMessagesOnly(): Boolean = prefEnabled("Allow Messages Only", false)
fun LauncherSettingsState.silentNotifications(): Boolean = prefEnabled("Silent Notifications", true)
fun LauncherSettingsState.hideNotificationIcons(): Boolean = prefEnabled("Hide Notification Icons", true)
fun LauncherSettingsState.disableNotificationBadges(): Boolean = prefEnabled("Disable Notification Badges", true)
fun LauncherSettingsState.disablePopups(): Boolean = prefEnabled("Disable Popups", true)
fun LauncherSettingsState.disableVibration(): Boolean = prefEnabled("Disable Vibration", true)
fun LauncherSettingsState.screenTimeCounter(): Boolean = prefEnabled("Screen Time Counter", true)
fun LauncherSettingsState.unlockCounter(): Boolean = prefEnabled("Unlock Counter", false)
fun LauncherSettingsState.focusHoursTracker(): Boolean = prefEnabled("Focus Hours Tracker", true)
fun LauncherSettingsState.dailyUsageGraph(): Boolean = prefEnabled("Daily Usage Graph", false)
fun LauncherSettingsState.appUsageStatistics(): Boolean = prefEnabled("App Usage Statistics", true)
fun LauncherSettingsState.relapseRiskDetection(): Boolean = prefEnabled("Relapse Risk Detection", false)
fun LauncherSettingsState.dopamineDetoxMode(): Boolean = prefEnabled("Dopamine Detox Mode", true)
fun LauncherSettingsState.weeklyUsageReport(): Boolean = prefEnabled("Weekly Usage Report", true)
fun LauncherSettingsState.disableInternetAccess(): Boolean = prefEnabled("Disable Internet Access", false)
fun LauncherSettingsState.blackAndWhiteOnly(): Boolean = if (!enforcementActive()) false else prefEnabled("Black & White Only", true)
fun LauncherSettingsState.disableEntertainmentApps(): Boolean = if (!enforcementActive()) false else prefEnabled("Disable Entertainment Apps", true)
fun LauncherSettingsState.hideAllNonEssentialApps(): Boolean = if (!enforcementActive()) false else prefEnabled("Hide All Non-Essential Apps", true)
fun LauncherSettingsState.lockDeviceDuringFocusSessions(): Boolean = prefEnabled("Lock Device During Focus Sessions", false)
fun LauncherSettingsState.calmUnlockScreen(): Boolean = prefEnabled("Calm Unlock Screen", true)
fun LauncherSettingsState.breathingUnlockAnimation(): Boolean = prefEnabled("Breathing Unlock Animation", false)
fun LauncherSettingsState.slowUnlockDelay(): Boolean = prefEnabled("Slow Unlock Delay", true)
fun LauncherSettingsState.textOnlyAppSearch(): Boolean = prefEnabled("Text-Only App Search", true)
fun LauncherSettingsState.disableAppIcons(): Boolean = prefEnabled("Disable App Icons", true)
fun LauncherSettingsState.gestureNavigation(): Boolean = prefEnabled("Gesture Navigation", true)
fun LauncherSettingsState.swipeDownForSearch(): Boolean = prefEnabled("Swipe Down For Search", true)
fun LauncherSettingsState.swipeLeftForNotes(): Boolean = prefEnabled("Swipe Left For Notes", true)
fun LauncherSettingsState.swipeRightForFocusMode(): Boolean = prefEnabled("Swipe Right For Focus Mode", true)
fun LauncherSettingsState.disableInfiniteScrolling(): Boolean = prefEnabled("Disable Infinite Scrolling", true)
fun LauncherSettingsState.verticalListNavigation(): Boolean = prefEnabled("Vertical List Navigation", true)
fun LauncherSettingsState.silentModeDefault(): Boolean = prefEnabled("Silent Mode Default", true)
fun LauncherSettingsState.minimalNotificationSounds(): Boolean = prefEnabled("Minimal Notification Sounds", true)
fun LauncherSettingsState.focusSounds(): Boolean = prefEnabled("Focus Sounds", false)
fun LauncherSettingsState.meditationAmbientSounds(): Boolean = prefEnabled("Meditation Ambient Sounds", false)
fun LauncherSettingsState.keyboardSoundOff(): Boolean = prefEnabled("Keyboard Sound Off", true)
fun LauncherSettingsState.nightSchedule(): Boolean = prefEnabled("Night Schedule", false)
fun LauncherSettingsState.extraDimDisplay(): Boolean = prefEnabled("Extra Dim Display", false)
fun LauncherSettingsState.warmGrayscale(): Boolean = prefEnabled("Warm Grayscale", false)
fun LauncherSettingsState.disableInternetAtNight(): Boolean = prefEnabled("Disable Internet At Night", false)
fun LauncherSettingsState.sleepReminder(): Boolean = prefEnabled("Sleep Reminder", false)
fun LauncherSettingsState.bedtimeLock(): Boolean = prefEnabled("Bedtime Lock", false)
fun LauncherSettingsState.disableBrightnessBoost(): Boolean = prefEnabled("Disable Brightness Boost", true)
fun LauncherSettingsState.pinProtectSettings(): Boolean = if (!enforcementActive()) false else prefEnabled("PIN Protect Settings", true)
fun LauncherSettingsState.preventLauncherExit(): Boolean = if (!enforcementActive()) false else prefEnabled("Prevent Launcher Exit", false)
fun LauncherSettingsState.setAsPermanentHomeApp(): Boolean = prefEnabled("Set As Permanent Home App", true)
fun LauncherSettingsState.kioskModeEnabledPref(): Boolean = prefEnabled("Kiosk Mode", false)
fun LauncherSettingsState.lockSafeModeAccess(): Boolean = prefEnabled("Lock Safe Mode Access", true)
fun LauncherSettingsState.hideSensitiveApps(): Boolean = if (!enforcementActive()) false else prefEnabled("Hide Sensitive Apps", true)
fun LauncherSettingsState.emergencyContactAccess(): Boolean = prefEnabled("Emergency Contact Access", true)
fun LauncherSettingsState.breathingExerciseScreen(): Boolean = prefEnabled("Breathing Exercise Screen", true)
fun LauncherSettingsState.focusTimer(): Boolean = prefEnabled("Focus Timer", true)
fun LauncherSettingsState.pomodoroMode(): Boolean = prefEnabled("Pomodoro Mode", false)
fun LauncherSettingsState.meditationTimer(): Boolean = prefEnabled("Meditation Timer", false)
fun LauncherSettingsState.calmQuotes(): Boolean = prefEnabled("Calm Quotes", true)
fun LauncherSettingsState.blankScreenMode(): Boolean = prefEnabled("Blank Screen Mode", false)
fun LauncherSettingsState.ambientRainSounds(): Boolean = prefEnabled("Ambient Rain Sounds", false)
fun LauncherSettingsState.deepWorkSessionTimer(): Boolean = prefEnabled("Deep Work Session Timer", true)
fun LauncherSettingsState.appOpeningDelayChoice(): String = prefChoice("App Opening Delay", "3 sec")
fun LauncherSettingsState.statusBarChoice(): String = prefChoice("Status Bar", "Show")
fun LauncherSettingsState.grayscaleModeChoice(): String = prefChoice("Grayscale Mode", "Always On")
fun LauncherSettingsState.fontStyleChoice(): String = prefChoice("Font Style", "Inter")
fun LauncherSettingsState.fontSizeChoice(): String = prefChoice("Font Size", "Medium")
fun LauncherSettingsState.uiDensityChoice(): String = prefChoice("UI Density", "Minimal")
fun LauncherSettingsState.animationLevelChoice(): String = prefChoice("Animation Level", "Soft Fade")
fun LauncherSettingsState.themeChoice(): String = prefChoice("Theme", "Pure Black")
fun LauncherSettingsState.wallpaperChoice(): String = prefChoice("Wallpaper", "Solid Black")
fun LauncherSettingsState.notificationModeChoice(): String = prefChoice("Notification Mode", "Disable All")
fun LauncherSettingsState.notificationDigestTimeChoice(): String = prefChoice("Notification Digest Time", "21:00")
fun LauncherSettingsState.dailyPhoneLimitChoice(): String = prefChoice("Daily Phone Limit", "120 min")
fun LauncherSettingsState.alarmSoundStyleChoice(): String = prefChoice("Alarm Sound Style", "Minimal")

fun LauncherSettingsState.homeUiSectionVisible(label: String): Boolean {
    return when (label) {
        "Show Time" -> showTime()
        "Show Date" -> showDate()
        "Show Battery %" -> showBattery()
        "Show Weather" -> showWeather()
        "Show Next Event" -> showNextEvent()
        "Minimal Clock Style" -> minimalClockStyle()
        "Large Clock Style" -> largeClockStyle()
        "Hide Search Bar" -> hideSearchBar()
        "Hide Navigation Bar" -> hideNavigationBar()
        "Disable Widgets" -> disableWidgets()
        else -> true
    }
}

fun LauncherSettingsState.appOpeningDelaySeconds(label: String): Int {
    val baseDelay = when (appOpeningDelayChoice()) {
        "Off" -> 0
        "3 sec" -> 3
        "5 sec" -> 5
        "10 sec" -> 10
        else -> 3
    }
    val frictionDelay = when (frictionLevelChoiceValue()) {
        "Monk" -> 4
        "Hardcore Monk" -> 8
        else -> 0
    }
    val modeDelay = if (analogMode()) 2 else 0
    val breathDelay = if (breathUnlock()) 4 else 0
    val einkDelay = if (eInkSimulation()) 1 else 0
    val slowModeDelay = if (slowModeEnabled()) maxOf(baseDelay, 6) else baseDelay
    return when {
        blockAppInstallation() && isStoreLabel(label) -> maxOf(slowModeDelay, 10 + frictionDelay + breathDelay)
        appCooldownTimerEnabled() -> maxOf(slowModeDelay + frictionDelay + modeDelay + breathDelay + einkDelay, 8)
        dopamineDetectionEngine() && isAddictiveLabel(label) -> maxOf(slowModeDelay + frictionDelay + modeDelay + breathDelay + einkDelay, 6)
        else -> slowModeDelay + frictionDelay + modeDelay + breathDelay + einkDelay
    }
}

fun LauncherSettingsState.shouldDeadEndFeedFor(label: String): Boolean {
    return deadEndFeedsEnabled() && (isAddictiveLabel(label) || recoveryMode())
}

fun LauncherSettingsState.requiresLaunchConfirmation(label: String): Boolean {
    if (!enforcementActive()) return false
    return lockAppsEnabled() || requireIntentBeforeOpening() || intentBasedAppOpening() || requireLongPress() || requirePinForApps() || isSensitiveLabel(label) || breathUnlock() || oneAppAtATimeMode() || analogMode() || frictionLevelChoiceValue() != "Light"
}

fun LauncherSettingsState.isLaunchBlocked(label: String, screenTimeMinutes: Int): Boolean {
    if (!enforcementActive()) return false
    if (dailyUsageLimitsEnabled()) {
        val maxMinutes = dailyPhoneLimitChoice().substringBefore(" ").toIntOrNull() ?: 120
        if (screenTimeMinutes >= maxMinutes) {
            return true
        }
    }

    if (focusModeEnabledPref()) {
        if (allowCallsOnly() && !label.equals("Phone", ignoreCase = true)) {
            return true
        }
        if (allowMessagesOnly() && !label.equals("Messages", ignoreCase = true)) {
            return true
        }
        if (blockBrowser() && isBrowserLabel(label)) {
            return true
        }
        if (blockSocialMedia() && isSocialLabel(label)) {
            return true
        }
        if (blockPlayStore() && isStoreLabel(label)) {
            return true
        }
        if (disableEntertainmentApps() && isEntertainmentLabel(label)) {
            return true
        }
    }

    when (environmentModeChoiceValue()) {
        "Sleep" -> {
            if (!isSleepAllowedLabel(label)) return true
        }
        "Deep Work" -> {
            if (isBrowserLabel(label) || isSocialLabel(label) || isEntertainmentLabel(label) || isStoreLabel(label)) return true
        }
        "Outside", "Nature" -> {
            if (isNatureBlockedLabel(label)) return true
        }
        "Gym" -> {
            if (!isGymAllowedLabel(label)) return true
        }
        "Drive" -> {
            if (!isDriveAllowedLabel(label)) return true
        }
        "Minimal" -> {
            if (!isMinimalAllowedLabel(label)) return true
        }
        "Recovery" -> {
            if (isRecoveryBlockedLabel(label)) return true
        }
        "Social" -> {
            // Social mode allows social apps but keeps other distractions out.
            if (isEntertainmentLabel(label) || isStoreLabel(label)) return true
        }
    }

    if (hideSensitiveApps() && isSensitiveLabel(label)) {
        return true
    }

    return false
}

fun LauncherSettingsState.shouldShowAppInLists(label: String, focusModeActive: Boolean): Boolean {
    if (!enforcementActive()) return true
    if (!allowedAppsListEnabled()) {
        if (hideSensitiveApps() && isSensitiveLabel(label)) return false
        if (blockAppInstallation() && isStoreLabel(label)) return false
        return true
    }

    if (focusModeActive && focusModeAppsEnabled() && !isEssentialLabel(label)) {
        return false
    }

    when (environmentModeChoiceValue()) {
        "Sleep" -> return isSleepAllowedLabel(label)
        "Gym" -> return isGymAllowedLabel(label)
        "Drive" -> return isDriveAllowedLabel(label)
        "Minimal" -> return isMinimalAllowedLabel(label)
    }

    if (hiddenAppsEnabled() && !isEssentialLabel(label)) {
        return false
    }

    if (essentialAppsOnly() && !isEssentialLabel(label)) {
        return false
    }

    if (hideAllNonEssentialApps() && !isEssentialLabel(label)) {
        return false
    }

    if (blockAppInstallation() && isStoreLabel(label)) {
        return false
    }

    if (disableEntertainmentApps() && isEntertainmentLabel(label)) {
        return false
    }

    if (blockBrowser() && isBrowserLabel(label)) {
        return false
    }

    if (blockSocialMedia() && isSocialLabel(label)) {
        return false
    }

    if (hideSensitiveApps() && isSensitiveLabel(label)) {
        return false
    }

    if (invisibleSocialApps() && isSocialLabel(label)) {
        return false
    }

    if (minimalSocialLayer() && isSocialLabel(label)) {
        return false
    }

    when (environmentModeChoiceValue()) {
        "Deep Work" -> if (isBrowserLabel(label) || isSocialLabel(label) || isEntertainmentLabel(label) || isStoreLabel(label)) return false
        "Outside", "Nature" -> if (isNatureBlockedLabel(label)) return false
        "Recovery" -> if (isRecoveryBlockedLabel(label)) return false
        "Social" -> if (isEntertainmentLabel(label) || isStoreLabel(label)) return false
    }

    return true
}

fun LauncherSettingsState.shouldShowRecentApps(): Boolean = !disableRecentlyUsedApps()
fun LauncherSettingsState.shouldShowSuggestions(): Boolean = !disableAppSuggestions()
fun LauncherSettingsState.shouldUseDynamicMinimalUi(riskMessage: String): Boolean = ultraMinimalScreen() || environmentModeChoiceValue() == "Minimal" || (dynamicMinimalism() && riskMessage.isNotBlank())
fun LauncherSettingsState.showSearchOnlyText(): Boolean = textOnlyAppSearch()
fun LauncherSettingsState.useCompactDensity(): Boolean = uiDensityChoice().equals("Compact", ignoreCase = true)
fun LauncherSettingsState.useComfortableDensity(): Boolean = uiDensityChoice().equals("Comfortable", ignoreCase = true)
fun LauncherSettingsState.showDisabledNavigation(): Boolean = !hideNavigationBar()
fun LauncherSettingsState.useSystemMonochrome(): Boolean = blackAndWhiteOnly() || grayscaleModeChoice() != "Off"
fun LauncherSettingsState.useExtraDim(): Boolean = extraDimDisplay() || nightSchedule() || disableBrightnessBoost()
fun LauncherSettingsState.shouldSilentModeDefault(): Boolean = silentModeDefault() || nightSchedule()
fun LauncherSettingsState.shouldPromptForHomeRole(): Boolean = setAsPermanentHomeApp()
fun LauncherSettingsState.shouldAllowEmergencyBypass(): Boolean = emergencyBypassEnabled() && focusModeEnabledPref()
fun LauncherSettingsState.shouldQuietInteractions(): Boolean = disableVibration() || slowModeEnabled()
fun LauncherSettingsState.shouldShowAppSuggestions(): Boolean = shouldShowSuggestions()
fun LauncherSettingsState.shouldShowRecentAppStrip(): Boolean = shouldShowRecentApps()
fun LauncherSettingsState.shouldAutoOpenFocusMode(): Boolean = scheduleFocusMode() && focusModeEnabledPref()
fun LauncherSettingsState.shouldLockLauncherExit(): Boolean = kioskModeEnabledPref() || preventLauncherExit()
fun LauncherSettingsState.shouldShowToolEntry(): Boolean = breathingExerciseScreen() || focusTimer() || pomodoroMode() || meditationTimer() || calmQuotes() || blankScreenMode() || ambientRainSounds() || deepWorkSessionTimer() || slowModeEnabled() || usageReflectionScreen() || calmAIAssistant()

fun LauncherSettingsState.environmentModeChoiceValue(): String = prefChoice("Environment Mode", "Study")
fun LauncherSettingsState.frictionLevelChoiceValue(): String = prefChoice("Friction Level", "Light")

// Home UI preferences
fun LauncherSettingsState.showIconsOnHome(): Boolean = prefEnabled("Show Icons On Home", false)
fun LauncherSettingsState.compactHome(): Boolean = prefEnabled("Compact Home", true)

fun LauncherSettingsState.environmentModeLabel(): String {
    return when (environmentModeChoiceValue()) {
        "Sleep" -> "sleep mode"
        "Gym" -> "gym mode"
        "Deep Work" -> "deep work mode"
        "Outside", "Nature" -> "nature mode"
        "Drive" -> "drive mode"
        "Minimal" -> "minimal mode"
        "Recovery" -> "recovery mode"
        "Social" -> "social mode"
        else -> "study mode"
    }
}

fun LauncherSettingsState.shouldHideFromHomeSurface(label: String): Boolean {
    return invisibleSocialApps() && isSocialLabel(label)
}

fun LauncherSettingsState.filterAppItems(items: List<LauncherAppItem>, focusModeActive: Boolean): List<LauncherAppItem> {
    return items.filter { item -> shouldShowAppInLists(item.label, focusModeActive) }
}

private fun isEssentialLabel(label: String): Boolean {
    return essentialAppLabels.any { it.equals(label, ignoreCase = true) }
}

private fun isSleepAllowedLabel(label: String): Boolean {
    return listOf("Phone", "Messages", "Alarm", "Settings").any { it.equals(label, ignoreCase = true) }
}

private fun isGymAllowedLabel(label: String): Boolean {
    return listOf("Phone", "Messages", "Music", "Maps", "Camera", "Settings").any { it.equals(label, ignoreCase = true) }
}

// Drive mode: only navigation, music, and calls (plus Settings to switch modes). Texting is blocked.
private fun isDriveAllowedLabel(label: String): Boolean {
    return listOf("Phone", "Maps", "Music", "Settings").any { it.equals(label, ignoreCase = true) }
}

// Minimal mode (signature): only calls, SMS, notes, maps, camera, music, clock remain.
private fun isMinimalAllowedLabel(label: String): Boolean {
    return listOf("Phone", "Messages", "Notes", "Maps", "Camera", "Music", "Clock", "Alarm", "Timer", "Settings")
        .any { it.equals(label, ignoreCase = true) }
}

// Nature mode: outdoor focus — maps, weather, compass, camera, music, calls; no social/entertainment/browser.
private fun isNatureBlockedLabel(label: String): Boolean {
    return isSocialLabel(label) || isEntertainmentLabel(label) || isBrowserLabel(label)
}

// Recovery mode: digital detox — block every addictive surface.
private fun isRecoveryBlockedLabel(label: String): Boolean {
    return isAddictiveLabel(label) || isSocialLabel(label) || isEntertainmentLabel(label) ||
        isBrowserLabel(label) || isStoreLabel(label)
}

private fun isSensitiveLabel(label: String): Boolean {
    return label.equals("Settings", ignoreCase = true)
}

private fun isBrowserLabel(label: String): Boolean {
    return browserKeywords.any { label.contains(it, ignoreCase = true) }
}

private fun isSocialLabel(label: String): Boolean {
    return socialKeywords.any { label.contains(it, ignoreCase = true) }
}

private fun isStoreLabel(label: String): Boolean {
    return storeKeywords.any { label.contains(it, ignoreCase = true) }
}

private fun isEntertainmentLabel(label: String): Boolean {
    return entertainmentKeywords.any { label.contains(it, ignoreCase = true) }
}

private fun isAddictiveLabel(label: String): Boolean {
    return addictiveKeywords.any { label.contains(it, ignoreCase = true) }
}
