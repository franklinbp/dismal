package com.dismal.app.data.repository

import com.dismal.app.data.db.ProductDao
import com.dismal.app.data.network.StoreApi
import com.dismal.app.data.network.dto.LicenseDto
import com.dismal.app.data.network.dto.ProductDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class StoreRepositoryTest {
    private val storeApi = mockk<StoreApi>()
    private val productDao = mockk<ProductDao>(relaxed = true)

    @Test
    fun refreshProducts_insertsItems() =
        runTest {
            val products =
                listOf(
                    ProductDto(
                        id = "p1",
                        name = "Prod 1",
                        description = "Desc",
                        price = 10.0,
                        platform = "web",
                        imageUrl = null,
                        licenses = emptyList<LicenseDto>(),
                    ),
                )
            coEvery { storeApi.getProducts() } returns Response.success(products)

            val repo = StoreRepository(storeApi, productDao)
            val result = repo.refreshProducts()

            coVerify { productDao.insertProducts(match { it.size == 1 }) }
            assertTrue(result is com.dismal.app.data.common.AppResult.Success)
        }

    @Test
    fun refreshProducts_throwsOnError() =
        runTest {
            coEvery { storeApi.getProducts() } returns
                Response.error(
                    500,
                    "".toResponseBody("text/plain".toMediaTypeOrNull()),
                )

            val repo = StoreRepository(storeApi, productDao)
            val result = repo.refreshProducts()

            assertTrue(result is com.dismal.app.data.common.AppResult.Error)
        }
}
