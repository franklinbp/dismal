package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Customer(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(ignore = true) val isSynced: Boolean = false,
    @Json(ignore = true) val saldoActual: Double? = null,
    @Json(ignore = true) val diasAtraso: Long = 0L,
    @Json(ignore = true) val estado: String? = null,
    @Json(ignore = true) val syncStatus: String = "SYNCED",
    @Json(ignore = true) val syncErrorMessage: String? = null,
)
