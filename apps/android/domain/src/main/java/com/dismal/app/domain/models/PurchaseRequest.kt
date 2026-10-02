package com.dismal.app.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PurchaseRequest(
    @Json(name = "productId") val productId: String,
)
