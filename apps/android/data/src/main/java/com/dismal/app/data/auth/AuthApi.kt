package com.dismal.app.data.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/authenticate")
    suspend fun authenticate(
        @Body request: AuthRequest,
    ): Response<AuthResponse>
}
