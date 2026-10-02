package com.dismal.app.ui.pricelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.AdminCatalogRepository
import com.dismal.app.data.repository.PriceListRepository
import com.dismal.app.domain.models.AdminProduct
import com.dismal.app.domain.models.PriceListSendRequest
import com.dismal.app.domain.models.PriceListSendResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PriceListViewModel
    @Inject
    constructor(
        private val adminCatalogRepository: AdminCatalogRepository,
        private val priceListRepository: PriceListRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(PriceListUiState())
            private set

        init {
            refreshProducts()
        }

        fun refreshProducts() {
            if (uiState.isLoading) return
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            viewModelScope.launch {
                when (val result = adminCatalogRepository.getProducts()) {
                    is AppResult.Success ->
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                products = result.data,
                            )
                    is AppResult.Error ->
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = result.message,
                            )
                }
            }
        }

        fun clearFeedback() {
            uiState = uiState.copy(errorMessage = null, successMessage = null)
        }

        fun sendByEmail(
            clientName: String,
            email: String,
            subject: String,
            includePdf: Boolean,
            includeXlsx: Boolean,
            includeEcFinalPrice: Boolean,
            includeEcDistributorPrice: Boolean,
            includePeFinalPrice: Boolean,
            includePeDistributorPrice: Boolean,
            includeStock: Boolean,
        ) {
            sendPriceList(
                PriceListSendRequest(
                    toEmail = email.trim(),
                    subject = subject.trim(),
                    clientName = clientName.trim().takeIf { it.isNotBlank() },
                    includePdf = includePdf,
                    includeXlsx = includeXlsx,
                    includeEcFinalPrice = includeEcFinalPrice,
                    includeEcDistributorPrice = includeEcDistributorPrice,
                    includePeFinalPrice = includePeFinalPrice,
                    includePeDistributorPrice = includePeDistributorPrice,
                    includeStock = includeStock,
                ),
                "Lista enviada por email.",
            )
        }

        fun sendByWhatsapp(
            clientName: String,
            whatsappPhone: String,
            includeEcFinalPrice: Boolean,
            includeEcDistributorPrice: Boolean,
            includePeFinalPrice: Boolean,
            includePeDistributorPrice: Boolean,
            includeStock: Boolean,
        ) {
            sendPriceList(
                PriceListSendRequest(
                    clientName = clientName.trim().takeIf { it.isNotBlank() },
                    whatsappPhone = whatsappPhone.trim(),
                    includePdf = false,
                    includeXlsx = false,
                    includeEcFinalPrice = includeEcFinalPrice,
                    includeEcDistributorPrice = includeEcDistributorPrice,
                    includePeFinalPrice = includePeFinalPrice,
                    includePeDistributorPrice = includePeDistributorPrice,
                    includeStock = includeStock,
                    sendWhatsapp = true,
                ),
                "Lista enviada por WhatsApp.",
            )
        }

        private fun sendPriceList(
            request: PriceListSendRequest,
            successMessage: String,
        ) {
            if (uiState.isSending) return
            uiState = uiState.copy(isSending = true, errorMessage = null, successMessage = null)
            viewModelScope.launch {
                when (val result = priceListRepository.sendPriceList(request)) {
                    is AppResult.Success ->
                        uiState =
                            uiState.copy(
                                isSending = false,
                                lastResult = result.data,
                                successMessage = successMessage,
                            )
                    is AppResult.Error ->
                        uiState =
                            uiState.copy(
                                isSending = false,
                                errorMessage = result.message,
                            )
                }
            }
        }
    }

data class PriceListUiState(
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val products: List<AdminProduct> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastResult: PriceListSendResult? = null,
)

