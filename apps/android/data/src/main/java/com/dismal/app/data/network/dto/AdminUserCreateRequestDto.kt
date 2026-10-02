package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminUserCreateRequestDto(
    @Json(name = "firstname") val firstname: String,
    @Json(name = "lastname") val lastname: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "role") val role: String = "CUSTOMER",
    @Json(name = "customerType") val customerType: String = "FINAL",
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "password") val password: String? = null,
)

