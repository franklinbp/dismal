package com.dismal.app.ui.sales

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.AdminCatalogRepository
import com.dismal.app.data.repository.CustomerRepository
import com.dismal.app.data.repository.InventoryRepository
import com.dismal.app.data.repository.SaleRepository
import com.dismal.app.domain.models.AdminProduct
import com.dismal.app.domain.models.Customer
import com.dismal.app.domain.models.Sale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SaleViewModel
    @Inject
    constructor(
        private val saleRepository: SaleRepository,
        private val customerRepository: CustomerRepository,
        private val adminCatalogRepository: AdminCatalogRepository,
        private val inventoryRepository: InventoryRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(SaleUiState())
            private set

        init {
            observeSales()
            observeCustomers()
            observeInventory()
            refreshData()
        }

        fun refreshData() {
            refreshSales()
            refreshCustomers()
            refreshProducts()
            refreshInventory()
        }

        fun addSale(
            clientId: String,
            softwareId: String,
            quantity: Int,
            unitPrice: Double,
            saleType: String,
            country: String,
            deliveryChannels: List<String>,
            registerFullPayment: Boolean,
            paymentMethod: String,
            paymentReference: String?,
            onSuccess: () -> Unit = {},
        ) {
            if (uiState.isSubmitting) return
            viewModelScope.launch {
                uiState = uiState.copy(isSubmitting = true, errorMessage = null, successMessage = null)
                when (val stockResult = inventoryRepository.getAvailableActivationCount(softwareId)) {
                    is AppResult.Success -> {
                        val available = stockResult.data
                        uiState =
                            uiState.copy(
                                inventoryLoaded = true,
                                availableActivationsByProductId =
                                    uiState.availableActivationsByProductId + (softwareId to available),
                            )
                        if (available < quantity) {
                            uiState =
                                uiState.copy(
                                    isSubmitting = false,
                                    errorMessage =
                                        "No se puede confirmar la venta: quedan $available activacion(es) disponibles y estas intentando vender $quantity. Agrega inventario para este producto o selecciona otro.",
                                )
                            return@launch
                        }
                    }
                    is AppResult.Error -> Unit
                }
                when (
                    val result =
                        saleRepository.createAndConfirmSaleRemote(
                            clientId = clientId,
                            softwareId = softwareId,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            saleType = saleType,
                            country = country,
                            deliveryChannels = deliveryChannels,
                            registerFullPayment = registerFullPayment,
                            paymentMethod = paymentMethod,
                            paymentReference = paymentReference,
                        )
                ) {
                    is AppResult.Success -> {
                        refreshSales()
                        refreshInventory()
                        val reference = result.data.saleNumber ?: result.data.id.takeLast(6).uppercase()
                        uiState =
                            uiState.copy(
                                isSubmitting = false,
                                successMessage =
                                    if (registerFullPayment) {
                                        "Venta $reference confirmada, entregada y pagada."
                                    } else {
                                        "Venta $reference confirmada y procesada."
                                    },
                            )
                        onSuccess()
                    }
                    is AppResult.Error -> {
                        val saleWasPersisted =
                            result.message.startsWith("La venta ya fue creada") ||
                                result.message.startsWith("Venta confirmada")
                        if (saleWasPersisted) {
                            refreshSales()
                        }
                        refreshInventory()
                        uiState =
                            uiState.copy(
                                isSubmitting = false,
                                errorMessage = result.message,
                            )
                        if (saleWasPersisted) {
                            onSuccess()
                        }
                    }
                }
            }
        }

        fun clearFeedback() {
            uiState = uiState.copy(errorMessage = null, successMessage = null)
        }

        fun prepareNewSale(): Boolean {
            clearFeedback()
            val message =
                when {
                    uiState.customers.isEmpty() -> "No hay clientes disponibles. Actualiza los datos o registra un cliente primero."
                    uiState.products.isEmpty() -> "No hay productos disponibles. Actualiza el catálogo e inténtalo nuevamente."
                    else -> null
                }
            if (message != null) {
                uiState = uiState.copy(errorMessage = message)
                refreshData()
                return false
            }
            return true
        }

        private fun refreshSales() {
            viewModelScope.launch {
                when (val result = saleRepository.refreshSales()) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> uiState = uiState.copy(errorMessage = result.message)
                }
            }
        }

        private fun refreshCustomers() {
            viewModelScope.launch {
                when (val result = customerRepository.refreshCustomers()) {
                    is AppResult.Success -> Unit
                    is AppResult.Error -> uiState = uiState.copy(errorMessage = result.message)
                }
            }
        }

        private fun refreshProducts() {
            viewModelScope.launch {
                when (val result = adminCatalogRepository.getProducts()) {
                    is AppResult.Success -> uiState = uiState.copy(products = result.data)
                    is AppResult.Error -> uiState = uiState.copy(errorMessage = result.message)
                }
            }
        }

        private fun refreshInventory() {
            viewModelScope.launch {
                when (val result = inventoryRepository.refreshLicenses()) {
                    is AppResult.Success -> uiState = uiState.copy(inventoryLoaded = true)
                    is AppResult.Error -> Unit
                }
            }
        }

        private fun observeSales() {
            viewModelScope.launch {
                saleRepository.sales
                    .catch {
                        uiState = uiState.copy(errorMessage = it.message ?: "Error al cargar ventas.")
                    }.collect { sales ->
                        uiState = uiState.copy(sales = sales)
                    }
            }
        }

        private fun observeCustomers() {
            viewModelScope.launch {
                customerRepository.customers.collect { customers ->
                    uiState =
                        uiState.copy(
                            customers = customers,
                            customerNames = customers.associate { it.id to it.name },
                        )
                }
            }
        }

        private fun observeInventory() {
            viewModelScope.launch {
                inventoryRepository.licenses
                    .catch {
                        uiState = uiState.copy(inventoryLoaded = false)
                    }.collect { licenses ->
                        val stockByProduct =
                            licenses
                                .filter { license ->
                                    !license.softwareId.isNullOrBlank() &&
                                        license.available &&
                                        license.status.equals("ACTIVE", ignoreCase = true)
                                }.groupBy { license -> license.softwareId.orEmpty() }
                                .mapValues { entry ->
                                    entry.value.sumOf { license ->
                                        (license.maxActivations - license.usedActivations).coerceAtLeast(0)
                                    }
                                }
                        uiState =
                            uiState.copy(
                                availableActivationsByProductId = stockByProduct,
                            )
                    }
            }
        }
    }

data class SaleUiState(
    val sales: List<Sale> = emptyList(),
    val customerNames: Map<String, String> = emptyMap(),
    val customers: List<Customer> = emptyList(),
    val products: List<AdminProduct> = emptyList(),
    val availableActivationsByProductId: Map<String, Int> = emptyMap(),
    val inventoryLoaded: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isSubmitting: Boolean = false,
)
