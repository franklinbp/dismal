package com.dismal.app.ui.customers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.CustomerRepository
import com.dismal.app.domain.models.Customer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel
    @Inject
    constructor(
        private val customerRepository: CustomerRepository,
    ) : ViewModel() {
        var uiState by mutableStateOf(CustomerUiState())
            private set

        init {
            observeCustomers()
            refreshCustomers()
        }

        fun addCustomer(
            name: String,
            email: String,
            phone: String?,
            password: String?,
        ) {
            if (uiState.isSubmitting) return
            viewModelScope.launch {
                uiState = uiState.copy(isSubmitting = true, errorMessage = null, successMessage = null)
                when (val result = customerRepository.createCustomerRemote(name, email, phone, password)) {
                    is AppResult.Success -> {
                        refreshCustomers()
                        val safePassword = password?.takeIf { it.isNotBlank() } ?: "(default del sistema)"
                        uiState =
                            uiState.copy(
                                isSubmitting = false,
                                successMessage = "Cliente creado. Password inicial: $safePassword",
                            )
                    }
                    is AppResult.Error -> {
                        uiState =
                            uiState.copy(
                                isSubmitting = false,
                                errorMessage = result.message,
                            )
                    }
                }
            }
        }

        fun refreshCustomers() {
            if (uiState.isLoading) return
            viewModelScope.launch {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                when (val result = customerRepository.refreshCustomers()) {
                    is AppResult.Success -> {
                        uiState = uiState.copy(isLoading = false)
                    }
                    is AppResult.Error -> {
                        uiState = uiState.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            }
        }

        fun clearFeedback() {
            uiState = uiState.copy(errorMessage = null, successMessage = null)
        }

        private fun observeCustomers() {
            viewModelScope.launch {
                customerRepository.customers
                    .catch {
                        uiState =
                            uiState.copy(
                                isLoading = false,
                                errorMessage = it.message ?: "Error al cargar clientes.",
                            )
                    }.collect { customers ->
                        uiState = uiState.copy(customers = customers)
                    }
            }
        }
    }

data class CustomerUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val customers: List<Customer> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
