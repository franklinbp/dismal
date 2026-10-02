package com.dismal.app.features.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.repository.StoreRepository
import com.dismal.app.domain.models.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel
    @Inject
    constructor(
        private val storeRepository: StoreRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val productId: String = checkNotNull(savedStateHandle["productId"])

        private val _product = MutableStateFlow<Product?>(null)
        val product = _product.asStateFlow()

        private val _uiState = MutableStateFlow(ProductDetailUiState())
        val uiState = _uiState.asStateFlow()

        private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
        val purchaseState = _purchaseState.asStateFlow()

        init {
            viewModelScope.launch {
                storeRepository.products.collect { products ->
                    val found = products.find { it.id == productId }
                    _product.value = found
                    val error =
                        if (found == null && products.isNotEmpty()) {
                            "Producto no encontrado"
                        } else {
                            null
                        }
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error,
                        )
                }
            }
        }

        fun purchase() {
            viewModelScope.launch {
                _purchaseState.value = PurchaseState.Error("Compra directa no disponible en esta version.")
            }
        }
    }

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed class PurchaseState {
    object Idle : PurchaseState()

    object Loading : PurchaseState()

    data class Success(val message: String) : PurchaseState()

    data class Error(val message: String) : PurchaseState()
}
