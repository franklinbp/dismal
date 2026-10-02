package com.dismal.app.data.network

import com.dismal.app.data.network.dto.MarketingAnalysisDto
import com.dismal.app.data.network.dto.PageResponseDto
import com.dismal.app.data.network.dto.StrategyActionDto
import com.dismal.app.data.network.dto.StrategyActionStatusRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface MarketingIntelligenceApi {
    @GET("api/v1/marketing/intelligence/analysis")
    suspend fun getAnalysis(): Response<List<MarketingAnalysisDto>>

    @GET("api/v1/marketing/strategy/actions")
    suspend fun getStrategyActions(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): Response<PageResponseDto<StrategyActionDto>>

    @PATCH("api/v1/marketing/strategy/actions/{id}/status")
    suspend fun updateStrategyActionStatus(
        @Path("id") id: String,
        @Body request: StrategyActionStatusRequestDto,
    ): Response<StrategyActionDto>
}
