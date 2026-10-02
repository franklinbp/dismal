package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "platform") val platform: String,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "licenses") val licenses: List<LicenseDto> = emptyList(),
)
