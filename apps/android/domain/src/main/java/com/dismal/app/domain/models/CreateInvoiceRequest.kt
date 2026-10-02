package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateInvoiceRequest(
    @Json(name = "saleId") val saleId: String,
    @Json(name = "dueDate") val dueDate: String? = null,
)
