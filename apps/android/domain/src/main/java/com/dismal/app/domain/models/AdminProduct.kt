package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdminProduct(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String,
    @Json(name = "price") val price: Double,
    @Json(name = "platform") val platform: String,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "ecFinalPrice") val ecFinalPrice: Double? = null,
    @Json(name = "ecDistributorPrice") val ecDistributorPrice: Double? = null,
    @Json(name = "peFinalPrice") val peFinalPrice: Double? = null,
    @Json(name = "peDistributorPrice") val peDistributorPrice: Double? = null,
)

