package com.dismal.app.ui.customers

import com.dismal.app.TestDispatcherRule
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.CustomerRepository
import com.dismal.app.domain.models.Customer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CustomerViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val customerRepository = mockk<CustomerRepository>()

    @Test
    fun `emits customers from repository`() =
        runTest {
            val customersFlow = MutableSharedFlow<List<Customer>>(replay = 1)
            val customers =
                listOf(
                    Customer(
                        id = "1",
                        name = "John Doe",
                        email = "john.doe@example.com",
                        phone = "1234567890",
                    ),
                )
            every { customerRepository.customers } returns customersFlow
            coEvery { customerRepository.refreshCustomers() } returns AppResult.Success(Unit)

            val viewModel = CustomerViewModel(customerRepository)
            customersFlow.emit(customers)

            assertEquals(1, viewModel.uiState.customers.size)
            assertEquals("John Doe", viewModel.uiState.customers.first().name)
        }
}
