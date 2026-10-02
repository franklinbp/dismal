package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class License(
    @Json(name = "id") val id: String,
    @Json(name = "licenseKey") val licenseKey: String,
    @Json(name = "softwareId") val softwareId: String? = null,
    @Json(name = "softwareName") val softwareName: String? = null,
    @Json(name = "purchasePrice") val purchasePrice: Double,
    @Json(name = "maxActivations") val maxActivations: Int,
    @Json(name = "usedActivations") val usedActivations: Int,
    @Json(name = "status") val status: String,
    @Json(name = "ownerId") val ownerId: String? = null,
    @Json(name = "ownerEmail") val ownerEmail: String? = null,
    @Json(name = "available") val available: Boolean,
)
