package com.dismal.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val syncRepository: SyncRepository,
        private val sessionManager: SessionManager,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            if (!sessionManager.hasToken() || sessionManager.isOfflineMode()) {
                return Result.success()
            }
            return when (val result = syncRepository.syncAll()) {
                is AppResult.Success -> Result.success()
                is AppResult.Error -> {
                    if (result.message.contains("operaciones con errores", ignoreCase = true)) {
                        Result.success()
                    } else {
                        Result.retry()
                    }
                }
            }
        }
    }
