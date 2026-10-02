package com.dismal.app.ui.collections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.AccountsReceivableRepository
import com.dismal.app.data.repository.CustomerRepository
import com.dismal.app.data.repository.SaleRepository
import com.dismal.app.domain.models.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val accountsReceivableRepository: AccountsReceivableRepository,
    private val customerRepository: CustomerRepository,
    private val saleRepository: SaleRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {
    var uiState by mutableStateOf(CollectionsUiState())
        private set

    init {
        uiState = uiState.copy(offlineMode = sessionManager.isOfflineMode())
        observeCustomers()
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            customerRepository.customers
                .catch {
                    uiState = uiState.copy(errorMessage = it.message ?: "Error al cargar clientes.")
                }
                .collect { customers ->
                    uiState = uiState.copy(customers = customers)
                }
        }
    }

    fun registerPayment(
        customerId: String,
        amount: Double,
        method: String,
        reference: String?,
    ) {
        if (amount <= 0.0) {
            uiState = uiState.copy(errorMessage = "El monto debe ser mayor a cero.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(isSubmitting = true, errorMessage = null, successMessage = null)
            when (val arResult = accountsReceivableRepository.getOpenByClient(customerId)) {
                is AppResult.Error -> {
                    uiState = uiState.copy(isSubmitting = false, errorMessage = arResult.message)
                }
                is AppResult.Success -> {
                    var remaining = amount
                    var paidCount = 0
                    for (ar in arResult.data) {
                        if (remaining <= 0.0) break
                        val amountToPay = remaining.coerceAtMost(ar.balance)
                        when (
                            val paymentResult =
                                saleRepository.registerPaymentRemote(
                                    saleId = ar.saleId,
                                    amount = amountToPay,
                                    method = method,
                                    reference = reference?.takeIf { it.isNotBlank() } ?: "MOBILE_COLLECTION",
                                )
                        ) {
                            is AppResult.Success -> {
                                remaining -= amountToPay
                                paidCount += 1
                            }
                            is AppResult.Error -> {
                                uiState =
                                    uiState.copy(
                                        isSubmitting = false,
                                        errorMessage = paymentResult.message,
                                    )
                                return@launch
                            }
                        }
                    }
                    customerRepository.refreshCustomers()
                    uiState =
                        uiState.copy(
                            isSubmitting = false,
                            successMessage = if (paidCount > 0) "Cobro registrado correctamente." else null,
                            errorMessage = if (paidCount == 0) "No hay cuentas por cobrar abiertas para este cliente." else null,
                        )
                }
            }
        }
    }
}

data class CollectionsUiState(
    val customers: List<Customer> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val offlineMode: Boolean = false,
    val isSubmitting: Boolean = false,
)
