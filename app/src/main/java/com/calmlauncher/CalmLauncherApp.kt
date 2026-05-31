package com.calmlauncher

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.calmlauncher.data.datastore.SettingsDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point. Hilt-enabled and provides the WorkManager configuration so
 * background workers (screen-time rollup, nightly reflection, risk evaluation) can be
 * constructor-injected.
 */
@HiltAndroidApp
class CalmLauncherApp : Application(), Configuration.Provider {

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var settingsDataStore: SettingsDataStore

	private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()
		appScope.launch {
			settingsDataStore.purgeLegacyKeys()
		}
	}

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
			.build()
}
