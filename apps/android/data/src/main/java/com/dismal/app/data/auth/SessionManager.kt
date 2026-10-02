package com.dismal.app.data.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager
    @Inject
    constructor(
        private val tokenStore: TokenStore,
    ) {
        fun hasToken(): Boolean = tokenStore.hasToken()

        fun currentRole(): String? = tokenStore.getRole()

        fun currentEmail(): String? = tokenStore.getEmail()

        fun isOfflineMode(): Boolean = tokenStore.isOfflineMode()

        fun resolveStartDestination(): String = if (hasToken()) "dashboard" else "login"

        fun canAccessAdmin(): Boolean = hasToken() && isPrivilegedRole(currentRole())

        fun canUseOfflineLogin(): Boolean = hasToken() && isPrivilegedRole(currentRole())

        fun hasCustomOfflinePin(): Boolean = tokenStore.getOfflinePin() != TokenStore.DEFAULT_OFFLINE_PIN

        fun storeToken(token: String) {
            tokenStore.setToken(token)
        }

        fun completeOnlineSession(
            email: String?,
            role: String?,
        ) {
            if (!email.isNullOrBlank()) {
                tokenStore.setEmail(email)
            }
            if (!role.isNullOrBlank()) {
                tokenStore.setRole(role)
            }
            tokenStore.setOfflineMode(false)
            tokenStore.resetOfflineSecurity()
        }

        fun activateOfflineMode() {
            tokenStore.setOfflineMode(true)
        }

        fun clearOfflineMode() {
            tokenStore.setOfflineMode(false)
        }

        fun getOfflinePin(): String = tokenStore.getOfflinePin()

        fun setOfflinePin(pin: String) {
            tokenStore.setOfflinePin(pin)
        }

        fun getOfflineAttempts(): Int = tokenStore.getOfflineAttempts()

        fun getOfflineLockUntil(): Long = tokenStore.getOfflineLockUntil()

        fun registerOfflineFailure(
            maxAttempts: Int,
            lockMinutes: Int,
        ) {
            tokenStore.registerOfflineFailure(maxAttempts, lockMinutes)
        }

        fun resetOfflineSecurity() {
            tokenStore.resetOfflineSecurity()
        }

        fun getLastSyncAt(): Long = tokenStore.getLastSyncAt()

        fun getLastSyncStatus(): String? = tokenStore.getLastSyncStatus()

        fun getLastSyncMessage(): String? = tokenStore.getLastSyncMessage()

        fun setLastSync(
            status: String,
            message: String?,
            pendingCount: Int,
            errorCount: Int,
        ) {
            tokenStore.setLastSync(status, message, pendingCount, errorCount)
        }

        fun clearSession() {
            tokenStore.clearToken()
        }

        fun isPrivilegedRole(role: String?): Boolean {
            return role?.uppercase() in setOf("ADMIN", "MANAGER")
        }
    }
