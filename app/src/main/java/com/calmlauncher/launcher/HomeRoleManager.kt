package com.calmlauncher.launcher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles becoming (and detecting) the default home / launcher app. Uses the modern
 * [RoleManager] ROLE_HOME flow on Android 10+ (works on Samsung One UI), with a
 * fallback to the home-settings screen.
 */
@Singleton
class HomeRoleManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val roleManager: RoleManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else {
            null
        }

    fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.let {
                if (it.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return it.isRoleHeld(RoleManager.ROLE_HOME)
                }
            }
        }
        // Fallback: resolve the current home and compare package.
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, 0)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /**
     * Intent to request that the user make Calm the default home app. Returns the
     * RoleManager request on Q+ when available, otherwise the home settings screen.
     */
    fun requestDefaultHomeIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager?.let {
                if (it.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return it.createRequestRoleIntent(RoleManager.ROLE_HOME)
                }
            }
        }
        return Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
    }
}
