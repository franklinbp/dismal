package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArSummaryDto(
    @Json(name = "saldoActual") val saldoActual: Double?,
    @Json(name = "diasAtraso") val diasAtraso: Long?,
    @Json(name = "estado") val estado: String?,
)
