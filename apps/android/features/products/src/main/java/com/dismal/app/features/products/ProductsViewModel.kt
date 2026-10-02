package com.dismal.app.features.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.repository.StoreRepository
import com.dismal.app.domain.models.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel
    @Inject
    constructor(
        private val storeRepository: StoreRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(ProductsUiState())
            private set

        init {
            viewModelScope.launch {
                storeRepository.products
                    .onStart {
                        android.util.Log.d("ProductsViewModel", "Starting to collect products from DB.")
                        uiState = uiState.copy(isLoading = true)
                    }
                    .catch {
                        android.util.Log.e("ProductsViewModel", "Error collecting from DB: ${it.message}", it)
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = it.message ?: "Error al cargar productos desde la base de datos",
                            )
                    }
                    .collect { products ->
                        android.util.Log.d("ProductsViewModel", "Collected ${products.size} products from DB. Updating UI state.")
                        uiState = uiState.copy(isLoading = false, items = products)
                    }
            }
        }

        // Removed local refreshProducts() to enforce centralized sync strategy via Dashboard

        fun onQueryChange(value: String) {
            uiState = uiState.copy(query = value)
        }

        fun onPlatformChange(value: String?) {
            uiState = uiState.copy(selectedPlatform = value)
        }

        fun onMinPriceChange(value: String) {
            uiState = uiState.copy(minPrice = value)
        }

        fun onMaxPriceChange(value: String) {
            uiState = uiState.copy(maxPrice = value)
        }
    }

data class ProductsUiState(
    val isLoading: Boolean = false,
    val items: List<Product> = emptyList(),
    val errorMessage: String? = null,
    val query: String = "",
    val selectedPlatform: String? = null,
    val minPrice: String = "",
    val maxPrice: String = "",
)
