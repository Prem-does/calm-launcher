package com.calmlauncher.data.system

import android.content.Context
import android.content.Intent
import com.calmlauncher.domain.model.LauncherTool
import com.calmlauncher.domain.service.AppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens apps via [android.content.pm.PackageManager] launch intents, preferring resolved
 * Samsung/Google packages for built-in tools and falling back to generic action intents.
 * Every launch is wrapped so a missing/blocked app returns false instead of crashing.
 */
@Singleton
class AppLauncherImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: LauncherAppCatalog,
) : AppLauncher {

    override fun canLaunch(packageName: String): Boolean = runCatching {
        context.packageManager.getLaunchIntentForPackage(packageName) != null
    }.getOrDefault(false)

    override fun launch(packageName: String): Boolean {
        val intent = runCatching {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }.getOrNull() ?: return false
        return startActivity(intent)
    }

    override fun launchTool(tool: LauncherTool): Boolean {
        // Prefer a concrete installed package.
        resolveToolPackage(tool)?.let { pkg ->
            if (launch(pkg)) return true
        }
        // Fall back to a generic action intent the system can resolve.
        val fallback = SamsungPackages.intentFallback(tool) ?: return false
        return startActivity(fallback)
    }

    override fun resolveToolPackage(tool: LauncherTool): String? = catalog.resolveTool(tool)

    private fun startActivity(intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}
