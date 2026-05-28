package com.calmlauncher.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.calmlauncher.data.db.CalmDatabaseProvider
import com.calmlauncher.launcher.LauncherSettingsState
import com.calmlauncher.launcher.blockAppInstallation
import com.calmlauncher.launcher.blockBrowser
import com.calmlauncher.launcher.blockPlayStore
import com.calmlauncher.launcher.blockSocialMedia
import com.calmlauncher.launcher.disableEntertainmentApps
import com.calmlauncher.launcher.dopamineDetectionEngine
import com.calmlauncher.launcher.isLaunchBlocked
import com.calmlauncher.launcher.oneAppAtATimeMode
import com.calmlauncher.launcher.recoveryMode
import kotlinx.coroutines.flow.firstOrNull

data class PlatformGuardDecision(
    val shouldReturnHome: Boolean,
    val reason: String
)

object PlatformGuardPolicy {
    private val browserPackages = listOf("chrome", "firefox", "browser", "brave", "edge")
    private val socialPackages = listOf("instagram", "twitter", "facebook", "threads", "snapchat", "reddit", "tiktok")
    private val storePackages = listOf("vending", "packageinstaller", "installer", "market")
    private val entertainmentPackages = listOf("youtube", "netflix", "spotify", "video", "music", "prime")

    suspend fun settings(context: Context): LauncherSettingsState {
        val entity = CalmDatabaseProvider.get(context).settingsDao().observeSettings().firstOrNull()
        return LauncherSettingsState(
            pinHash = entity?.pinHash,
            pinProtected = entity?.pinProtected ?: false,
            grayscaleForced = entity?.grayscaleForced ?: true,
            kioskModeEnabled = entity?.kioskModeEnabled ?: false,
            hiddenStatusBar = entity?.hiddenStatusBar ?: false,
            focusModeEnabled = entity?.focusModeEnabled ?: false,
            preferences = parsePreferencesBlob(entity?.preferencesBlob.orEmpty())
        )
    }

    fun decide(
        context: Context,
        packageName: String,
        settings: LauncherSettingsState,
        previousExternalPackage: String?
    ): PlatformGuardDecision {
        if (packageName.isBlank() || packageName == context.packageName) {
            return PlatformGuardDecision(false, "")
        }

        val label = appLabel(context, packageName)
        val normalizedPackage = packageName.lowercase()
        val normalizedLabel = label.lowercase()
        val isStore = storePackages.any { normalizedPackage.contains(it) || normalizedLabel.contains(it) }
        val isBrowser = browserPackages.any { normalizedPackage.contains(it) || normalizedLabel.contains(it) }
        val isSocial = socialPackages.any { normalizedPackage.contains(it) || normalizedLabel.contains(it) }
        val isEntertainment = entertainmentPackages.any { normalizedPackage.contains(it) || normalizedLabel.contains(it) }

        if (settings.isLaunchBlocked(label, screenTimeMinutes = 0)) {
            return PlatformGuardDecision(true, "$label is blocked by launcher focus rules.")
        }
        if (settings.blockAppInstallation() && (isStore || normalizedPackage.contains("packageinstaller"))) {
            return PlatformGuardDecision(true, "App installation is blocked.")
        }
        if ((settings.blockBrowser() || settings.recoveryMode()) && isBrowser) {
            return PlatformGuardDecision(true, "Browser access is blocked.")
        }
        if ((settings.blockSocialMedia() || settings.recoveryMode()) && isSocial) {
            return PlatformGuardDecision(true, "Social apps are blocked.")
        }
        if (settings.blockPlayStore() && isStore) {
            return PlatformGuardDecision(true, "App stores are blocked.")
        }
        if (settings.disableEntertainmentApps() && isEntertainment) {
            return PlatformGuardDecision(true, "Entertainment apps are blocked.")
        }
        if (settings.oneAppAtATimeMode() && previousExternalPackage != null && previousExternalPackage != packageName) {
            return PlatformGuardDecision(true, "One-App-At-A-Time mode returned you home.")
        }
        return PlatformGuardDecision(false, "")
    }

    fun appLabel(context: Context, packageName: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName.substringAfterLast('.')
        }
    }

    fun openLauncher(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shouldWatchScroll(settings: LauncherSettingsState): Boolean {
        return settings.dopamineDetectionEngine() || settings.recoveryMode()
    }

    private fun parsePreferencesBlob(blob: String): Map<String, String> {
        if (blob.isBlank()) return emptyMap()
        return blob.lineSequence().mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
        }.toMap()
    }
}
