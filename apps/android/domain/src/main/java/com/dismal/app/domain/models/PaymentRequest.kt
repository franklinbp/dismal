package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentRequest(
    @Json(name = "saleId") val saleId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "method") val method: String,
    @Json(name = "reference") val reference: String? = null,
)
