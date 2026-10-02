package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MarketingAnalysis(
    @Json(name = "productId") val productId: String,
    @Json(name = "productName") val productName: String,
    @Json(name = "state") val state: String,
    @Json(name = "diagnosis") val diagnosis: String,
    @Json(name = "priority") val priority: String,
    @Json(name = "suggestedActions") val suggestedActions: List<String> = emptyList(),
    @Json(name = "explanation") val explanation: String,
)
