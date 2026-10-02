package com.dismal.app.data.network

import com.dismal.app.data.network.dto.DashboardSummaryDto
import com.dismal.app.data.network.dto.DashboardTargetDto
import com.dismal.app.data.network.dto.PageResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    @GET("api/v1/dashboard/admin/summary")
    suspend fun getSummary(): Response<DashboardSummaryDto>

    @GET("api/v1/dashboard/admin/sales-targets")
    suspend fun getSalesTargets(
        @Query("sort") sort: String = "profit",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
    ): Response<PageResponseDto<DashboardTargetDto>>
}

