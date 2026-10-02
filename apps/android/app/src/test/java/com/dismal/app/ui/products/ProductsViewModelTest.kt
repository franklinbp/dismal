package com.dismal.app.ui.products

import com.dismal.app.TestDispatcherRule
import com.dismal.app.data.common.AppResult
import com.dismal.app.data.repository.StoreRepository
import com.dismal.app.domain.models.Product
import com.dismal.app.features.products.ProductsViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProductsViewModelTest {
    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val storeRepository = mockk<StoreRepository>()

    @Test
    fun emitsProductsFromRepository() =
        runTest {
            val productsFlow = MutableSharedFlow<List<Product>>(replay = 1)
            val products =
                listOf(
                    Product(
                        id = "p1",
                        name = "Prod",
                        description = "Desc",
                        price = 10.0,
                        platform = "web",
                        imageUrl = null,
                        licenses = emptyList(),
                    ),
                )
            every { storeRepository.products } returns productsFlow
            coEvery { storeRepository.refreshProducts() } returns AppResult.Success(Unit)

            val viewModel = ProductsViewModel(storeRepository)
            productsFlow.emit(products)

            assertEquals(1, viewModel.uiState.items.size)
            assertEquals("p1", viewModel.uiState.items.first().id)
        }
}
