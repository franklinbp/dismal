package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PriceListSendResult(
    @Json(name = "sentAt") val sentAt: String,
    @Json(name = "whatsappText") val whatsappText: String? = null,
    @Json(name = "whatsappUrl") val whatsappUrl: String? = null,
    @Json(name = "emailSent") val emailSent: Boolean,
    @Json(name = "whatsappSent") val whatsappSent: Boolean,
)

