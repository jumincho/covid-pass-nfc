package com.jumincho.cvpass

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Owns the [AppContainer] and applies the retention policy on every start. */
class CvPassApplication : Application() {

    /** Dependencies shared by all screens. */
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
            Log.w(TAG, "Background work failed", error)
        },
    )

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, AppConfig.fromBuildConfig())
        applicationScope.launch { container.checkInService.purgeExpired() }
    }

    private companion object {
        const val TAG = "CvPass"
    }
}

/** The running app's [AppContainer], for `viewModelFactory { initializer { … } }` blocks. */
val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as CvPassApplication).container
