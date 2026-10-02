package com.dismal.app.data.network

import com.dismal.app.domain.models.PriceListSendRequest
import com.dismal.app.domain.models.PriceListSendResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportsApi {
    @POST("api/v1/reports/price-list/send")
    suspend fun sendPriceList(
        @Body request: PriceListSendRequest,
    ): Response<PriceListSendResult>
}

