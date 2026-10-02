package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.db.ProductDao
import com.dismal.app.data.db.asDatabaseModel
import com.dismal.app.data.db.asDomainModel
import com.dismal.app.data.network.StoreApi
import com.dismal.app.data.network.dto.asDomainModel
import com.dismal.app.domain.models.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoreRepository(
    private val storeApi: StoreApi,
    private val productDao: ProductDao,
) {
    val products: Flow<List<Product>> =
        productDao.getAllProducts().map {
            it.map { entity ->
                entity.asDomainModel()
            }
        }

    suspend fun refreshProducts(): AppResult<Unit> {
        return when (
            val result =
                safeApiCall(
                    errorPrefix = "Error al obtener productos",
                    call = { storeApi.getProducts() },
                ) { body ->
                    body.orEmpty().map { it.asDomainModel() }
                }
        ) {
            is AppResult.Success -> {
                productDao.insertProducts(result.data.map { product -> product.asDatabaseModel(isSynced = true) })
                AppResult.Success(Unit)
            }
            is AppResult.Error -> result
        }
    }
}
