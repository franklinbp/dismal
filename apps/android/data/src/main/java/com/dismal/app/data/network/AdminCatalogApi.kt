package com.dismal.app.data.network

import com.dismal.app.data.network.dto.AdminProductDto
import retrofit2.Response
import retrofit2.http.GET

interface AdminCatalogApi {
    @GET("api/v1/store/products")
    suspend fun getProducts(): Response<List<AdminProductDto>>
}

