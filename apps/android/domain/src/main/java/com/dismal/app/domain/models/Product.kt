package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Product(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "price") val price: Double,
    @Json(name = "platform") val platform: String,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "licenses") val licenses: List<License> = emptyList(),
)
