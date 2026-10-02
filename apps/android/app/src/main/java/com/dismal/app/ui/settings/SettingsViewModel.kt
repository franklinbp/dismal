package com.dismal.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dismal.app.BuildConfig
import com.dismal.app.data.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        var uiState by mutableStateOf(buildState())
            private set

        fun refresh() {
            uiState = buildState()
        }

        fun disableOfflineMode() {
            sessionManager.clearOfflineMode()
            uiState = buildState()
        }

        private fun buildState(): SettingsUiState {
            return SettingsUiState(
                apiBaseUrl = BuildConfig.API_BASE_URL,
                offlineMode = sessionManager.isOfflineMode(),
                hasToken = sessionManager.hasToken(),
                lastSyncAt = sessionManager.getLastSyncAt(),
                lastSyncStatus = sessionManager.getLastSyncStatus(),
                lastSyncMessage = sessionManager.getLastSyncMessage(),
            )
        }
    }

data class SettingsUiState(
    val apiBaseUrl: String = "",
    val offlineMode: Boolean = false,
    val hasToken: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncStatus: String? = null,
    val lastSyncMessage: String? = null,
)
