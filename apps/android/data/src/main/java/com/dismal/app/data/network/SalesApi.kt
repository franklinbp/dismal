package com.dismal.app.data.network

import com.dismal.app.data.network.dto.PageResponseDto
import com.dismal.app.data.network.dto.ConfirmSaleRequestDto
import com.dismal.app.domain.models.CreateSaleRequest
import com.dismal.app.domain.models.Sale
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SalesApi {
    @GET("api/v1/sales")
    suspend fun getSales(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
    ): Response<PageResponseDto<Sale>>

    @POST("api/v1/sales")
    suspend fun createSale(
        @Body request: CreateSaleRequest,
    ): Response<Sale>

    @POST("api/v1/sales/{id}/confirm")
    suspend fun confirmSale(
        @Path("id") saleId: String,
        @Body request: ConfirmSaleRequestDto = ConfirmSaleRequestDto(),
    ): Response<Sale>
}
