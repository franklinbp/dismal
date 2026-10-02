package com.dismal.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dismal.app.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DismalApp : Application(), Configuration.Provider {
    private companion object {
        const val TAG = "DismalApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        runCatching {
            SyncScheduler.schedulePeriodic(this)
        }.onFailure { error ->
            Log.e(TAG, "Unable to schedule background sync on startup", error)
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
