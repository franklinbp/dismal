package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateSaleRequest(
    @Json(name = "clientId") val clientId: String,
    @Json(name = "saleType") val saleType: String,
    @Json(name = "country") val country: String? = null,
    @Json(name = "items") val items: List<SaleItemRequest>,
)

@JsonClass(generateAdapter = true)
data class SaleItemRequest(
    @Json(name = "softwareId") val softwareId: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "unitPrice") val unitPrice: Double,
)
