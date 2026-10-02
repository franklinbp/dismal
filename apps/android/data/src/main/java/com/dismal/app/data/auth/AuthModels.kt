package com.dismal.app.data.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "token") val token: String? = null,
    @Json(name = "accessToken") val accessToken: String? = null,
)
