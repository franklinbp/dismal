package com.dismal.app.data.user

import retrofit2.Response
import retrofit2.http.GET

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun getMe(): Response<UserMe>

    @GET("api/v1/integrations/crm/status")
    suspend fun getCrmIntegrationStatus(): Response<CrmIntegrationStatus>
}
