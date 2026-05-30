package com.calmlauncher.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.calmlauncher.domain.model.AppCategory
import com.calmlauncher.domain.model.AppEntry
import com.calmlauncher.domain.model.LauncherTool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live view of launchable apps on the device, backed by [PackageManager]. The launcher's
 * own package is excluded. Categorisation is a best-effort heuristic combining
 * [ApplicationInfo.category] with curated package-prefix lists. Every PackageManager call
 * is wrapped so enumeration can never crash the launcher.
 */
@Singleton
class LauncherAppCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val pm: PackageManager get() = context.packageManager
    private val selfPackage: String get() = context.packageName

    /** All launchable apps (ACTION_MAIN + CATEGORY_LAUNCHER), excluding this launcher. */
    fun installedApps(): List<AppEntry> {
        val resolved = queryLauncherActivities()
        val seen = HashSet<String>()
        val result = ArrayList<AppEntry>(resolved.size)
        for (info in resolved) {
            val appInfo = info.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName ?: continue
            if (pkg == selfPackage) continue
            if (!seen.add(pkg)) continue // one entry per package
            val label = runCatching { info.loadLabel(pm)?.toString() }.getOrNull()
                ?: runCatching { pm.getApplicationLabel(appInfo).toString() }.getOrNull()
                ?: pkg
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            result += AppEntry(
                packageName = pkg,
                label = label,
                category = categorizeInfo(appInfo),
                isSystem = isSystem,
            )
        }
        return result.sortedBy { it.label.lowercase() }
    }

    /** Drawable icon for [packageName], or null if it can't be loaded. */
    fun loadIcon(packageName: String): Drawable? = runCatching {
        pm.getApplicationIcon(packageName)
    }.getOrNull()

    /** Best-effort behavioural category for an installed package. */
    fun categorize(packageName: String): AppCategory {
        val appInfo = runCatching {
            pm.getApplicationInfo(packageName, 0)
        }.getOrNull() ?: return knownCategoryFor(packageName) ?: AppCategory.OTHER
        return categorizeInfo(appInfo)
    }

    /** First installed package from this tool's preference list, or null if none present. */
    fun resolveTool(tool: LauncherTool): String? =
        SamsungPackages.candidatesFor(tool).firstOrNull { isInstalled(it) }

    fun isInstalled(packageName: String): Boolean = runCatching {
        pm.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)

    // -- internals ---------------------------------------------------------------

    private fun queryLauncherActivities(): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        }.getOrDefault(emptyList())
    }

    private fun categorizeInfo(appInfo: ApplicationInfo): AppCategory {
        // 1) Curated package lists win — they're more specific than the OS hint.
        knownCategoryFor(appInfo.packageName)?.let { return it }

        // 2) System-declared category (API 26+).
        when (appInfo.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_AUDIO -> return AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAME
            ApplicationInfo.CATEGORY_NEWS -> return AppCategory.ENTERTAINMENT
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.TOOL
            ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TOOL
        }

        // 3) Legacy "is a game" flag.
        @Suppress("DEPRECATION")
        if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) return AppCategory.GAME

        return AppCategory.OTHER
    }

    /** Matches a package against curated prefix lists. Returns null when unknown. */
    private fun knownCategoryFor(packageName: String): AppCategory? {
        val pkg = packageName.lowercase()
        fun anyMatch(prefixes: Array<String>) = prefixes.any { pkg == it || pkg.startsWith("$it.") }

        return when {
            anyMatch(SOCIAL_PACKAGES) -> AppCategory.SOCIAL
            anyMatch(BROWSER_PACKAGES) -> AppCategory.BROWSER
            anyMatch(STORE_PACKAGES) -> AppCategory.STORE
            anyMatch(ENTERTAINMENT_PACKAGES) -> AppCategory.ENTERTAINMENT
            anyMatch(GAME_PACKAGES) -> AppCategory.GAME
            anyMatch(COMMUNICATION_PACKAGES) -> AppCategory.COMMUNICATION
            anyMatch(TOOL_PACKAGES) -> AppCategory.TOOL
            else -> null
        }
    }

    private companion object {
        val SOCIAL_PACKAGES = arrayOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.facebook.lite",
            "com.zhiliaoapp.musically", // TikTok
            "com.ss.android.ugc.trill", // TikTok (intl)
            "com.twitter.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.pinterest",
            "com.linkedin.android",
            "com.tumblr",
            "com.bsky.app", // Bluesky
            "com.discord",
        )
        val COMMUNICATION_PACKAGES = arrayOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.facebook.orca", // Messenger
            "org.thoughtcrime.securesms", // Signal
            "com.viber.voip",
            "jp.naver.line.android",
            "com.skype.raider",
            "com.google.android.gm", // Gmail
            "com.microsoft.office.outlook",
            "com.samsung.android.email.provider",
        )
        val BROWSER_PACKAGES = arrayOf(
            "com.android.chrome",
            "com.chrome.beta",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.microsoft.emmx", // Edge
            "com.duckduckgo.mobile.android",
            "com.sec.android.app.sbrowser", // Samsung Internet
            "com.UCMobile.intl",
        )
        val STORE_PACKAGES = arrayOf(
            "com.android.vending", // Play Store
            "com.amazon.mShop.android.shopping",
            "com.sec.android.app.samsungapps", // Galaxy Store
            "com.ebay.mobile",
            "com.alibaba.aliexpresshd",
            "com.einnovation.temu",
            "com.contextlogic.wish",
            "com.shopee.id",
        )
        val ENTERTAINMENT_PACKAGES = arrayOf(
            "com.google.android.youtube",
            "com.netflix.mediaclient",
            "com.spotify.music",
            "com.amazon.avod.thirdpartyclient",
            "com.disney.disneyplus",
            "tv.twitch.android.app",
            "com.hulu.plus",
            "com.google.android.apps.youtube.music",
            "com.soundcloud.android",
            "com.hbo.hbonow",
            "com.wbd.stream", // Max
        )
        val GAME_PACKAGES = arrayOf(
            "com.king.candycrushsaga",
            "com.supercell.clashofclans",
            "com.supercell.clashroyale",
            "com.mojang.minecraftpe",
            "com.roblox.client",
            "com.epicgames.fortnite",
            "com.activision.callofduty.shooter",
        )
        val TOOL_PACKAGES = arrayOf(
            "com.google.android.calculator",
            "com.sec.android.app.popupcalculator",
            "com.google.android.calendar",
            "com.samsung.android.calendar",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.google.android.keep",
            "com.samsung.android.app.notes",
            "com.google.android.apps.maps",
            "com.android.settings",
            "com.google.android.GoogleCamera",
            "com.sec.android.app.camera",
        )
    }
}
