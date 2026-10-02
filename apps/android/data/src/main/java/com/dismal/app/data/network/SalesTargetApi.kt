package com.dismal.app.data.network

import com.dismal.app.data.network.dto.SalesTargetDto
import com.dismal.app.data.network.dto.SalesTargetSummaryDto
import retrofit2.Response
import retrofit2.http.GET

interface SalesTargetApi {
    @GET("api/v1/sales-targets")
    suspend fun getSalesTargets(): Response<List<SalesTargetDto>>

    @GET("api/v1/sales-targets/summary")
    suspend fun getSummary(): Response<SalesTargetSummaryDto>
}
