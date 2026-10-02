package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AccountsReceivable(
    @Json(name = "id") val id: String,
    @Json(name = "saleId") val saleId: String,
    @Json(name = "clientId") val clientId: String,
    @Json(name = "total") val total: Double,
    @Json(name = "paid") val paid: Double,
    @Json(name = "balance") val balance: Double,
    @Json(name = "dueDate") val dueDate: String? = null,
    @Json(name = "status") val status: String,
)
