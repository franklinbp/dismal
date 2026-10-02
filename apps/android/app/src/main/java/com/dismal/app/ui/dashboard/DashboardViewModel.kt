package com.dismal.app.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.DashboardRepository
import com.dismal.app.data.user.UserRepository
import com.dismal.app.domain.models.DashboardSummary
import com.dismal.app.domain.models.DashboardTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val userRepository: UserRepository,
        private val dashboardRepository: DashboardRepository,
    ) : ViewModel() {
        var uiState by
            mutableStateOf(
                DashboardUiState(
                    email = sessionManager.currentEmail(),
                    role = sessionManager.currentRole(),
                ),
            )
            private set

        init {
            refreshOverview()
        }

        fun visibleMenuItems(): List<DashboardMenuItem> {
            val role = uiState.role?.uppercase().orEmpty()
            return if (role == ROLE_ADMIN || role == ROLE_MANAGER) {
                allMenuItems
            } else {
                listOf(
                    DashboardMenuItem(DashboardRoutes.PROFILE, "Perfil"),
                    DashboardMenuItem(DashboardRoutes.LOGOUT, "Salir"),
                )
            }
        }

        fun refreshOverview() {
            if (uiState.isRefreshing) return
            if (!canAccessAdmin()) {
                uiState = uiState.copy(errorMessage = "Acceso disponible solo para administracion.")
                return
            }

            uiState = uiState.copy(isRefreshing = true, errorMessage = null)
            viewModelScope.launch {
                val summaryResult = dashboardRepository.getSummary()
                val targetsResult = dashboardRepository.getTopTargets()
                val summary =
                    (summaryResult as? AppResult.Success)?.data
                        ?: uiState.summary
                        ?: DashboardSummary.empty()

                uiState =
                    uiState.copy(
                        isRefreshing = false,
                        summary = summary,
                        targets = (targetsResult as? AppResult.Success)?.data.orEmpty(),
                        errorMessage =
                            listOfNotNull(
                                (targetsResult as? AppResult.Error)?.message,
                            ).firstOrNull(),
                    )
            }
        }

        fun requestLogout() {
            sessionManager.clearSession()
            userRepository.clearSession()
            uiState = uiState.copy(logoutRequested = true)
        }

        fun onLogoutHandled() {
            uiState = uiState.copy(logoutRequested = false)
        }

        fun clearMessage() {
            uiState = uiState.copy(errorMessage = null)
        }

        private fun canAccessAdmin(): Boolean {
            return sessionManager.canAccessAdmin()
        }

        companion object {
            private const val ROLE_ADMIN = "ADMIN"
            private const val ROLE_MANAGER = "MANAGER"
        }
    }

data class DashboardUiState(
    val isRefreshing: Boolean = false,
    val email: String? = null,
    val role: String? = null,
    val errorMessage: String? = null,
    val logoutRequested: Boolean = false,
    val summary: DashboardSummary? = null,
    val targets: List<DashboardTarget> = emptyList(),
)

data class DashboardMenuItem(
    val route: String,
    val label: String,
)

object DashboardRoutes {
    const val HOME = "home"
    const val CUSTOMERS = "customers"
    const val PRICE_LIST = "priceList"
    const val SALES = "sales"
    const val REPORTS = "reports"
    const val MARKETING = "marketing"
    const val BILLING = "billing"
    const val COLLECTIONS = "collections"
    const val PROFILE = "profile"
    const val LOGOUT = "logout"
}

private val allMenuItems =
    listOf(
        DashboardMenuItem(DashboardRoutes.HOME, "Dashboard"),
        DashboardMenuItem(DashboardRoutes.CUSTOMERS, "Clientes"),
        DashboardMenuItem(DashboardRoutes.PRICE_LIST, "Lista"),
        DashboardMenuItem(DashboardRoutes.SALES, "Ventas"),
        DashboardMenuItem(DashboardRoutes.REPORTS, "Reportes"),
        DashboardMenuItem(DashboardRoutes.MARKETING, "Estrategia"),
        DashboardMenuItem(DashboardRoutes.BILLING, "Facturas"),
        DashboardMenuItem(DashboardRoutes.COLLECTIONS, "Cobros"),
        DashboardMenuItem(DashboardRoutes.PROFILE, "Perfil"),
        DashboardMenuItem(DashboardRoutes.LOGOUT, "Salir"),
    )
