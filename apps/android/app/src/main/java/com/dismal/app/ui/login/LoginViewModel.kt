package com.dismal.app.ui.login

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.BuildConfig
import com.dismal.app.data.auth.AuthApi
import com.dismal.app.data.auth.AuthRequest
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val authApi: AuthApi,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(LoginUiState())
            private set

        var navigateToDashboard by mutableStateOf(false)
            private set

        fun onEmailChange(value: String) {
            uiState = uiState.copy(email = value)
        }

        fun onPasswordChange(value: String) {
            uiState = uiState.copy(password = value)
        }

        fun login() {
            if (uiState.isLoading) return

            val email = uiState.email.trim()
            val password = uiState.password
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            viewModelScope.launch {
                try {
                    Log.i(TAG, "Login baseUrl=${BuildConfig.API_BASE_URL}")
                    val authResponse = authApi.authenticate(AuthRequest(email, password))
                    if (authResponse.isSuccessful) {
                        val body = authResponse.body()
                        val rawToken = body?.token ?: body?.accessToken
                        val token = rawToken?.removePrefix("Bearer ")?.trim()
                        if (!token.isNullOrBlank()) {
                            sessionManager.storeToken(token)
                            when (val result = userRepository.refreshProfile()) {
                                is com.dismal.app.data.common.AppResult.Success -> {
                                    val role = result.data.role.uppercase()
                                    if (!sessionManager.isPrivilegedRole(role)) {
                                        sessionManager.clearSession()
                                        userRepository.clearSession()
                                        uiState =
                                            uiState.copy(
                                                isLoading = false,
                                                errorMessage = "Acceso disponible solo para administracion.",
                                            )
                                    } else {
                                        sessionManager.completeOnlineSession(
                                            email = result.data.email,
                                            role = result.data.role,
                                        )
                                        navigateToDashboard = true
                                        uiState = uiState.copy(isLoading = false, errorMessage = null)
                                    }
                                }
                                is com.dismal.app.data.common.AppResult.Error -> {
                                    sessionManager.clearSession()
                                    userRepository.clearSession()
                                    uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                                }
                            }
                        } else {
                            uiState =
                                uiState.copy(
                                    isLoading = false,
                                    errorMessage = "Respuesta inválida del servidor",
                                )
                        }
                    } else {
                        val errorBody = authResponse.errorBody()?.string()
                        Log.e(
                            TAG,
                            "Login failed code=${authResponse.code()} message=${authResponse.message()} body=${errorBody ?: "empty"}",
                        )
                        val errorSnippet = errorBody?.trim()?.take(200)
                        val message =
                            when (authResponse.code()) {
                                401 -> "Credenciales invalidas"
                                502, 503, 504 -> "El servicio de Dismal no esta disponible temporalmente. Intenta nuevamente en unos minutos."
                                500 -> "El servidor encontro un error. Intenta nuevamente."
                                else -> "Error (${authResponse.code()})${if (!errorSnippet.isNullOrBlank()) ": $errorSnippet" else ""}"
                            }
                        uiState = uiState.copy(isLoading = false, errorMessage = message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Login exception=${e.message}", e)
                    if (sessionManager.canUseOfflineLogin()) {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = "No se pudo conectar al servidor. Usa Entrar offline para continuar.",
                            )
                    } else {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = "No se pudo conectar al servidor",
                            )
                    }
                }
            }
        }

        fun loginOffline(pin: String) {
            if (!sessionManager.canUseOfflineLogin()) {
                uiState = uiState.copy(errorMessage = "Acceso offline disponible solo para administracion.")
                return
            }
            if (!sessionManager.hasCustomOfflinePin()) {
                uiState = uiState.copy(errorMessage = "Configura un PIN offline desde Perfil antes de usar este modo.")
                return
            }
            val now = System.currentTimeMillis()
            val lockedUntil = sessionManager.getOfflineLockUntil()
            if (lockedUntil > now) {
                val remainingMinutes = ((lockedUntil - now) / 60000L).coerceAtLeast(1L)
                uiState =
                    uiState.copy(
                        errorMessage = "Modo offline bloqueado. Intenta en $remainingMinutes min.",
                    )
                return
            }

            val expectedPin = sessionManager.getOfflinePin()
            if (pin != expectedPin) {
                sessionManager.registerOfflineFailure(MAX_OFFLINE_ATTEMPTS, OFFLINE_LOCK_MINUTES)
                val attempts = sessionManager.getOfflineAttempts()
                val remaining = (MAX_OFFLINE_ATTEMPTS - attempts).coerceAtLeast(0)
                val locked = sessionManager.getOfflineLockUntil() > System.currentTimeMillis()
                uiState =
                    uiState.copy(
                        errorMessage =
                            if (locked) {
                                "Modo offline bloqueado. Intenta más tarde."
                            } else {
                                "PIN incorrecto. Intentos restantes: $remaining"
                            },
                    )
                return
            }

            sessionManager.resetOfflineSecurity()
            sessionManager.activateOfflineMode()
            navigateToDashboard = true
            uiState = uiState.copy(errorMessage = null)
        }

        fun onNavigationHandled() {
            navigateToDashboard = false
        }

        companion object {
            private const val TAG = "LoginViewModel"
            private const val MAX_OFFLINE_ATTEMPTS = 5
            private const val OFFLINE_LOCK_MINUTES = 15
        }
    }
