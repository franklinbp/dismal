package com.dismal.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.user.UserRepository
import com.dismal.app.data.user.CrmIntegrationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(
            ProfileUiState(
                email = userRepository.cachedUser()?.email ?: sessionManager.currentEmail(),
                role = userRepository.cachedUser()?.role ?: sessionManager.currentRole(),
            ),
        )
            private set

        init {
            loadCrmStatus()
        }

        fun loadProfile() {
            if (uiState.isLoading) return
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            viewModelScope.launch {
                when (val result = userRepository.refreshProfile()) {
                    is AppResult.Success -> {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                email = result.data.email,
                                role = result.data.role,
                            )
                    }
                    is AppResult.Error -> {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = result.message,
                            )
                    }
                }
                loadCrmStatus()
            }
        }

        private fun loadCrmStatus() {
            if (uiState.isLoadingCrmStatus) return
            uiState = uiState.copy(isLoadingCrmStatus = true, crmStatusError = null)
            viewModelScope.launch {
                when (val result = userRepository.getCrmIntegrationStatus()) {
                    is AppResult.Success -> {
                        uiState = uiState.copy(isLoadingCrmStatus = false, crmStatus = result.data)
                    }
                    is AppResult.Error -> {
                        uiState = uiState.copy(
                            isLoadingCrmStatus = false,
                            crmStatus = null,
                            crmStatusError = result.message,
                        )
                    }
                }
            }
        }

        fun onOfflinePinChange(value: String) {
            if (value.length > 4 || value.any { !it.isDigit() }) return
            uiState = uiState.copy(offlinePin = value, offlinePinMessage = null)
        }

        fun onOfflinePinConfirmChange(value: String) {
            if (value.length > 4 || value.any { !it.isDigit() }) return
            uiState = uiState.copy(offlinePinConfirm = value, offlinePinMessage = null)
        }

        fun saveOfflinePin() {
            val pin = uiState.offlinePin
            val confirm = uiState.offlinePinConfirm
            if (pin.length != 4 || confirm.length != 4) {
                uiState = uiState.copy(offlinePinMessage = "El PIN debe tener 4 digitos.")
                return
            }
            if (pin != confirm) {
                uiState = uiState.copy(offlinePinMessage = "Los PIN no coinciden.")
                return
            }
            sessionManager.setOfflinePin(pin)
            sessionManager.resetOfflineSecurity()
            uiState =
                uiState.copy(
                    offlinePin = "",
                    offlinePinConfirm = "",
                    offlinePinMessage = "PIN actualizado.",
                )
        }
    }

data class ProfileUiState(
    val isLoading: Boolean = false,
    val email: String? = null,
    val role: String? = null,
    val errorMessage: String? = null,
    val offlinePin: String = "",
    val offlinePinConfirm: String = "",
    val offlinePinMessage: String? = null,
    val isLoadingCrmStatus: Boolean = false,
    val crmStatus: CrmIntegrationStatus? = null,
    val crmStatusError: String? = null,
)
