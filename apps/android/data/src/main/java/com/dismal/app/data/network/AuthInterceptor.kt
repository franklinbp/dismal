package com.dismal.app.data.network

import com.dismal.app.data.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getToken()
        val request =
            if (!token.isNullOrBlank()) {
                val authValue =
                    if (token.startsWith("Bearer ", ignoreCase = true)) {
                        token
                    } else {
                        "Bearer $token"
                    }
                chain.request().newBuilder()
                    .addHeader("Authorization", authValue)
                    .build()
            } else {
                chain.request()
            }
        return chain.proceed(request)
    }
}
