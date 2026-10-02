package com.dismal.app.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PurchaseResponse(
    @Json(name = "orderId") val orderId: String,
    @Json(name = "productName") val productName: String,
    @Json(name = "message") val message: String,
)
