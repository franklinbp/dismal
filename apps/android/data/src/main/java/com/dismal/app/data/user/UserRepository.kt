package com.dismal.app.data.user

import com.dismal.app.data.auth.TokenStore
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.formatHttpError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository
    @Inject
    constructor(
        private val userApi: UserApi,
        private val tokenStore: TokenStore,
    ) {
        private val userState = MutableStateFlow<UserMe?>(null)
        val currentUser: StateFlow<UserMe?> = userState

        fun cachedUser(): UserMe? = userState.value

        suspend fun refreshProfile(): AppResult<UserMe> {
            return try {
                val response = userApi.getMe()
                if (!response.isSuccessful) {
                    return AppResult.Error(formatHttpError("No se pudo cargar el perfil", response))
                }
                val me = response.body()
                if (me == null) {
                    return AppResult.Error("Respuesta inválida del servidor")
                }
                userState.value = me
                tokenStore.setRole(me.role)
                tokenStore.setEmail(me.email)
                AppResult.Success(me)
            } catch (e: Exception) {
                AppResult.Error("No se pudo conectar al servidor", e)
            }
        }

        suspend fun getCrmIntegrationStatus(): AppResult<CrmIntegrationStatus> {
            return try {
                val response = userApi.getCrmIntegrationStatus()
                if (!response.isSuccessful) {
                    return AppResult.Error(formatHttpError("No se pudo validar DismalCRM", response))
                }
                val status = response.body()
                    ?: return AppResult.Error("El servidor no devolvio el estado de DismalCRM")
                AppResult.Success(status)
            } catch (exception: Exception) {
                AppResult.Error("No se pudo consultar la conexion con DismalCRM", exception)
            }
        }

        fun clearSession() {
            userState.value = null
        }
    }
