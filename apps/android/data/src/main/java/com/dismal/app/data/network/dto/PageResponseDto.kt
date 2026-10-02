package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PageResponseDto<T>(
    @Json(name = "content") val content: List<T>,
    @Json(name = "totalElements") val totalElements: Long?,
    @Json(name = "totalPages") val totalPages: Int?,
    @Json(name = "size") val size: Int?,
    @Json(name = "number") val number: Int?,
)
