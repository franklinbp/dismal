package com.dismal.app.data.network

import com.dismal.app.domain.models.PaymentRequest
import com.dismal.app.domain.models.Sale
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentsApi {
    @POST("api/v1/payments")
    suspend fun addPayment(
        @Body request: PaymentRequest,
    ): Response<Sale>
}
