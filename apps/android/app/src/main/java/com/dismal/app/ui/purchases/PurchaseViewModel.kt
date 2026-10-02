package com.dismal.app.ui.purchases

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.SaleRepository
import com.dismal.app.domain.models.Sale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel
    @Inject
    constructor(
        private val saleRepository: SaleRepository,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        var uiState by mutableStateOf(PurchaseUiState())
            private set

        init {
            observePurchases()
        }

        fun refreshPurchases() {
            if (sessionManager.isOfflineMode()) {
                uiState = uiState.copy(isLoading = false, errorMessage = null)
                return
            }
            viewModelScope.launch {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                when (val result = saleRepository.refreshSales()) {
                    is AppResult.Success -> {
                        uiState = uiState.copy(isLoading = false)
                    }
                    is AppResult.Error -> {
                        uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }

        private fun observePurchases() {
            viewModelScope.launch {
                saleRepository.sales
                    .catch {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = it.message ?: "Error al cargar compras.",
                            )
                    }
                    .collect { sales ->
                        uiState = uiState.copy(purchases = sales) // purchases are sales here
                    }
            }
        }
    }

data class PurchaseUiState(
    val isLoading: Boolean = false,
    val purchases: List<Sale> = emptyList(), // purchases are sales here
    val errorMessage: String? = null,
)
