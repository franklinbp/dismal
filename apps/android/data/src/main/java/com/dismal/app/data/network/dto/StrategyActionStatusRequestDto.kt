package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StrategyActionStatusRequestDto(
    @Json(name = "status") val status: String,
    @Json(name = "resultNotes") val resultNotes: String? = null,
)
