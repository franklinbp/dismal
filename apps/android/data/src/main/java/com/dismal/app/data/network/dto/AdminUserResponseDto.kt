package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminUserResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "firstname") val firstname: String?,
    @Json(name = "lastname") val lastname: String?,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "arSummary") val arSummary: ArSummaryDto?,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "updated_at") val updatedAtAlt: String? = null,
)
