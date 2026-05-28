package com.calmlauncher.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.runtime.collectAsState
import com.calmlauncher.navigation.LauncherNavHost
import com.calmlauncher.ui.theme.CalmLauncherTheme

class LauncherActivity : ComponentActivity() {
    private val launcherStateViewModel by lazy {
        ViewModelProvider(this)[LauncherStateViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                launcherStateViewModel.startForegroundSession()
            }

            override fun onStop(owner: LifecycleOwner) {
                launcherStateViewModel.endForegroundSession()
            }
        })
        setContent {
            val uiState = launcherStateViewModel.uiState.collectAsState().value
            CalmLauncherTheme(settings = uiState.settings) {
                LauncherNavHost(launcherStateViewModel = launcherStateViewModel)
            }
        }
    }
}
