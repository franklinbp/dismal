package com.dismal.app.data.repository

import com.dismal.app.data.common.AppResult
import com.dismal.app.data.common.safeApiCall
import com.dismal.app.data.network.AdminCatalogApi
import com.dismal.app.data.network.dto.asDomainModel
import com.dismal.app.domain.models.AdminProduct

class AdminCatalogRepository(
    private val adminCatalogApi: AdminCatalogApi,
) {
    suspend fun getProducts(): AppResult<List<AdminProduct>> {
        return safeApiCall(
            errorPrefix = "No se pudo cargar el catalogo",
            call = { adminCatalogApi.getProducts() },
        ) { body ->
            body.orEmpty().map { it.asDomainModel() }
        }
    }
}
