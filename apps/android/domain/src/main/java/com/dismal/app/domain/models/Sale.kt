package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Sale(
    @Json(name = "id") val id: String,
    @Json(name = "saleNumber") val saleNumber: String? = null,
    @Json(name = "clientId") val clientId: String,
    @Json(name = "saleType") val saleType: String,
    @Json(name = "country") val country: String = "EC",
    @Json(name = "currency") val currency: String = "USD",
    @Json(name = "status") val status: String,
    @Json(name = "total") val total: Double,
    @Json(name = "paid") val paid: Double,
    @Json(name = "balance") val balance: Double,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "updatedAt") val updatedAt: String,
    @Json(name = "items") val items: List<SaleItem> = emptyList(),
    val syncStatus: String = "SYNCED",
    val syncErrorMessage: String? = null,
)

@JsonClass(generateAdapter = true)
data class SaleItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "softwareId") val softwareId: String,
    @Json(name = "softwareName") val softwareName: String? = null,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "unitPrice") val unitPrice: Double,
    @Json(name = "subtotal") val subtotal: Double? = null,
)
