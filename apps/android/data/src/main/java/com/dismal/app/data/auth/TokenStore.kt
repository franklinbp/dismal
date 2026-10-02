package com.dismal.app.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val masterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val prefs =
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun setToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    fun setRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun setEmail(email: String) {
        prefs.edit().putString(KEY_EMAIL, email).apply()
    }

    fun clearToken() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_ROLE)
            .remove(KEY_EMAIL)
            .remove(KEY_OFFLINE)
            .apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun isOfflineMode(): Boolean = prefs.getBoolean(KEY_OFFLINE, false)

    fun setOfflineMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE, enabled).apply()
    }

    fun getOfflinePin(): String {
        return prefs.getString(KEY_OFFLINE_PIN, DEFAULT_OFFLINE_PIN) ?: DEFAULT_OFFLINE_PIN
    }

    fun setOfflinePin(pin: String) {
        prefs.edit().putString(KEY_OFFLINE_PIN, pin).apply()
    }

    fun getOfflineAttempts(): Int = prefs.getInt(KEY_OFFLINE_ATTEMPTS, 0)

    fun getOfflineLockUntil(): Long = prefs.getLong(KEY_OFFLINE_LOCK_UNTIL, 0L)

    fun resetOfflineSecurity() {
        prefs.edit()
            .putInt(KEY_OFFLINE_ATTEMPTS, 0)
            .putLong(KEY_OFFLINE_LOCK_UNTIL, 0L)
            .apply()
    }

    fun registerOfflineFailure(maxAttempts: Int, lockMinutes: Int) {
        val attempts = getOfflineAttempts() + 1
        val editor = prefs.edit().putInt(KEY_OFFLINE_ATTEMPTS, attempts)
        if (attempts >= maxAttempts) {
            val lockUntil = System.currentTimeMillis() + lockMinutes * 60_000L
            editor.putLong(KEY_OFFLINE_LOCK_UNTIL, lockUntil)
        }
        editor.apply()
    }

    fun setLastSync(
        status: String,
        message: String?,
        pendingCount: Int,
        errorCount: Int,
    ) {
        prefs.edit()
            .putLong(KEY_LAST_SYNC_AT, System.currentTimeMillis())
            .putString(KEY_LAST_SYNC_STATUS, status)
            .putString(KEY_LAST_SYNC_MESSAGE, message)
            .putInt(KEY_LAST_SYNC_PENDING, pendingCount)
            .putInt(KEY_LAST_SYNC_ERRORS, errorCount)
            .apply()
    }

    fun getLastSyncAt(): Long = prefs.getLong(KEY_LAST_SYNC_AT, 0L)

    fun getLastSyncStatus(): String? = prefs.getString(KEY_LAST_SYNC_STATUS, null)

    fun getLastSyncMessage(): String? = prefs.getString(KEY_LAST_SYNC_MESSAGE, null)

    fun getLastSyncPending(): Int = prefs.getInt(KEY_LAST_SYNC_PENDING, 0)

    fun getLastSyncErrors(): Int = prefs.getInt(KEY_LAST_SYNC_ERRORS, 0)

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_ROLE = "user_role"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_OFFLINE = "offline_mode"
        private const val KEY_OFFLINE_PIN = "offline_pin"
        private const val KEY_OFFLINE_ATTEMPTS = "offline_attempts"
        private const val KEY_OFFLINE_LOCK_UNTIL = "offline_lock_until"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_LAST_SYNC_STATUS = "last_sync_status"
        private const val KEY_LAST_SYNC_MESSAGE = "last_sync_message"
        private const val KEY_LAST_SYNC_PENDING = "last_sync_pending"
        private const val KEY_LAST_SYNC_ERRORS = "last_sync_errors"
        const val DEFAULT_OFFLINE_PIN = "1234"
    }
}
