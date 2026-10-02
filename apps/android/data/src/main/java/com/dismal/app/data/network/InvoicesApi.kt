package com.dismal.app.data.network

import com.dismal.app.data.network.dto.PageResponseDto
import com.dismal.app.domain.models.CreateInvoiceRequest
import com.dismal.app.domain.models.Invoice
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface InvoicesApi {
    @GET("api/v1/invoices")
    suspend fun getInvoices(
        @Query("q") query: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 25,
    ): Response<PageResponseDto<Invoice>>

    @POST("api/v1/invoices")
    suspend fun createInvoice(
        @Body request: CreateInvoiceRequest,
    ): Response<Invoice>
}
