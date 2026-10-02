package com.dismal.app.data.network

import com.dismal.app.data.network.dto.LicenseDto
import com.dismal.app.data.network.dto.PageResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface InventoryApi {
    @GET("api/v1/inventory/licenses")
    suspend fun getLicenses(
        @Query("softwareId") softwareId: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): Response<PageResponseDto<LicenseDto>>
}
