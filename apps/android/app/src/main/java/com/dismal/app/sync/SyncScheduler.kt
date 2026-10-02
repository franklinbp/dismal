package com.dismal.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val UNIQUE_PERIODIC = "dismal_sync_periodic"
    private const val UNIQUE_ONCE = "dismal_sync_once"
    private const val PREFS = "sync_scheduler"
    private const val KEY_LAST_REQUEST_AT = "last_request_at"
    private const val FOREGROUND_COOLDOWN_MINUTES = 10L

    fun schedulePeriodic(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun requestOneTime(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONCE,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun requestForegroundSync(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastRequestAt = prefs.getLong(KEY_LAST_REQUEST_AT, 0L)
        if (now - lastRequestAt < TimeUnit.MINUTES.toMillis(FOREGROUND_COOLDOWN_MINUTES)) {
            return
        }
        prefs.edit().putLong(KEY_LAST_REQUEST_AT, now).apply()
        requestOneTime(context)
    }
}
