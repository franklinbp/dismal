package com.dismal.app.data.network

import com.dismal.app.domain.models.AccountsReceivable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AccountsReceivableApi {
    @GET("api/v1/ar/client/{clientId}")
    suspend fun getByClient(
        @Path("clientId") clientId: String,
    ): Response<List<AccountsReceivable>>
}
