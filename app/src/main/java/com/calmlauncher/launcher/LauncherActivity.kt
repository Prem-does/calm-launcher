package com.calmlauncher.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calmlauncher.core.designsystem.theme.CalmTheme
import com.calmlauncher.navigation.CalmRoot
import com.calmlauncher.work.CalmWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The launcher's only activity and the device HOME surface. Edge-to-edge, monochrome,
 * and Hilt-injected. Schedules background work on every start (idempotent) and hosts the
 * entire Compose tree via [CalmRoot].
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var workScheduler: CalmWorkScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        workScheduler.scheduleAll()
        setContent {
            CalmTheme {
                CalmRoot()
            }
        }
    }
}
