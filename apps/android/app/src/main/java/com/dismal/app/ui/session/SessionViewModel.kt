package com.dismal.app.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.dismal.app.data.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel
    @Inject
    constructor(
        sessionManager: SessionManager,
    ) : ViewModel() {
        var startDestination by mutableStateOf<String?>(null)
            private set

        init {
            startDestination = sessionManager.resolveStartDestination()
        }
    }
