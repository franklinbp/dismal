package com.dismal.app.ui.billing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.auth.SessionManager
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.InvoiceRepository
import com.dismal.app.data.repository.SaleRepository
import com.dismal.app.domain.models.Invoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel
    @Inject
    constructor(
        private val invoiceRepository: InvoiceRepository,
        private val saleRepository: SaleRepository,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        var uiState by mutableStateOf(BillingUiState())
            private set

        init {
            uiState = uiState.copy(offlineMode = sessionManager.isOfflineMode())
        }

        fun refreshInvoices() {
            refreshInvoices(clearNotice = true)
        }

        private fun refreshInvoices(clearNotice: Boolean) {
            if (sessionManager.isOfflineMode()) {
                uiState =
                    uiState.copy(
                        errorMessage = "Modo offline activo",
                        noticeMessage = if (clearNotice) null else uiState.noticeMessage,
                        offlineMode = true,
                    )
                return
            }
            viewModelScope.launch {
                uiState =
                    uiState.copy(
                        isLoading = true,
                        errorMessage = null,
                        noticeMessage = if (clearNotice) null else uiState.noticeMessage,
                    )
                when (val result = invoiceRepository.listInvoices()) {
                    is AppResult.Success -> {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                invoices = result.data,
                                errorMessage = null,
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
            }
        }

        fun createInvoiceFromSale(
            clientId: String,
            softwareId: String,
            quantity: Int,
            unitPrice: Double,
            saleType: String,
            dueDate: String?,
        ) {
            if (clientId.isBlank() || softwareId.isBlank()) {
                uiState = uiState.copy(errorMessage = "Cliente y software son requeridos")
                return
            }
            if (quantity <= 0 || unitPrice <= 0.0) {
                uiState = uiState.copy(errorMessage = "Cantidad y precio deben ser mayores a cero")
                return
            }
            if (sessionManager.isOfflineMode()) {
                uiState = uiState.copy(errorMessage = "Modo offline activo")
                return
            }
            viewModelScope.launch {
                uiState = uiState.copy(isLoading = true, errorMessage = null, noticeMessage = null)
                when (
                    val saleResult =
                        saleRepository.createSaleRemote(
                            clientId.trim(),
                            softwareId.trim(),
                            quantity,
                            unitPrice,
                            saleType.trim().uppercase(),
                        )
                ) {
                    is AppResult.Success -> {
                        val saleId = saleResult.data.id
                        when (val confirmResult = saleRepository.confirmSaleRemote(saleId)) {
                            is AppResult.Success -> {
                                when (val invoiceResult = invoiceRepository.createInvoice(saleId, dueDate?.trim())) {
                                    is AppResult.Success -> {
                                        uiState =
                                            uiState.copy(
                                                isLoading = false,
                                                noticeMessage = "Factura creada",
                                                errorMessage = null,
                                            )
                                        refreshInvoices(clearNotice = false)
                                    }
                                    is AppResult.Error -> {
                                        uiState =
                                            uiState.copy(
                                                isLoading = false,
                                                errorMessage = invoiceResult.message,
                                            )
                                    }
                                }
                            }
                            is AppResult.Error -> {
                                uiState =
                                    uiState.copy(
                                        isLoading = false,
                                        errorMessage = confirmResult.message,
                                    )
                            }
                        }
                    }
                    is AppResult.Error -> {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = saleResult.message,
                            )
                    }
                }
            }
        }
    }

data class BillingUiState(
    val isLoading: Boolean = false,
    val invoices: List<Invoice> = emptyList(),
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val offlineMode: Boolean = false,
)
