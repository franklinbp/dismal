package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PriceListSendRequest(
    @Json(name = "toEmail") val toEmail: String? = null,
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "clientName") val clientName: String? = null,
    @Json(name = "whatsappPhone") val whatsappPhone: String? = null,
    @Json(name = "includePdf") val includePdf: Boolean = true,
    @Json(name = "includeXlsx") val includeXlsx: Boolean = false,
    @Json(name = "includeEcFinalPrice") val includeEcFinalPrice: Boolean = true,
    @Json(name = "includeEcDistributorPrice") val includeEcDistributorPrice: Boolean = true,
    @Json(name = "includePeFinalPrice") val includePeFinalPrice: Boolean = true,
    @Json(name = "includePeDistributorPrice") val includePeDistributorPrice: Boolean = true,
    @Json(name = "includeStock") val includeStock: Boolean = false,
    @Json(name = "sendWhatsapp") val sendWhatsapp: Boolean = false,
    @Json(name = "countryCode") val countryCode: String? = null,
    @Json(name = "customerType") val customerType: String? = null,
)

