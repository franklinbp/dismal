package com.dismal.app.data.network

import com.dismal.app.data.network.dto.AdminUserResponseDto
import com.dismal.app.data.network.dto.AdminUserCreateRequestDto
import com.dismal.app.data.network.dto.PageResponseDto
import com.dismal.app.domain.models.Customer
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CustomerApi {
    @GET("api/v1/customers")
    suspend fun getCustomers(): Response<List<Customer>>

    @GET("api/v1/admin/users")
    suspend fun getAdminClients(
        @Query("type") type: String = "CLIENT",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): Response<PageResponseDto<AdminUserResponseDto>>

    @POST("api/v1/admin/users")
    suspend fun createAdminClient(
        @Body request: AdminUserCreateRequestDto,
    ): Response<AdminUserResponseDto>

    @POST("api/v1/customers/sync") // Assuming a sync endpoint for local creations
    suspend fun syncCustomers(
        @Body customers: List<Customer>,
    ): Response<List<Customer>> // Returns updated customers with server IDs/info
}
