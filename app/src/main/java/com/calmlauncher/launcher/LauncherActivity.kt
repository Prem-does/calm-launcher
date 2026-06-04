package com.calmlauncher.launcher

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.calmlauncher.navigation.CalmRoot
import com.calmlauncher.work.CalmWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The launcher's only activity and the device HOME surface. Edge-to-edge, monochrome,
 * and Hilt-injected. Schedules background work on every start (idempotent) and hosts the
 * entire Compose tree via [CalmRoot].
 */
@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LauncherActivity"
    }

    @Inject
    lateinit var workScheduler: CalmWorkScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        enableEdgeToEdge()
        setContent {
            CalmRoot()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            workScheduler.scheduleAll()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
    }
}
