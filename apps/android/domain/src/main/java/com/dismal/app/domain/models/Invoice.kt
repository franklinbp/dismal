package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Invoice(
    @Json(name = "id") val id: String,
    @Json(name = "invoiceNumber") val invoiceNumber: String,
    @Json(name = "status") val status: String,
    @Json(name = "issueDate") val issueDate: String,
    @Json(name = "dueDate") val dueDate: String,
    @Json(name = "totalAmount") val totalAmount: Double,
    @Json(name = "saleId") val saleId: String?,
    @Json(name = "clientId") val clientId: String? = null,
    @Json(name = "clientName") val clientName: String,
    @Json(name = "clientEmail") val clientEmail: String,
)
