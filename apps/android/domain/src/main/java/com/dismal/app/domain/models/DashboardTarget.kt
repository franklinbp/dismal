package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardTarget(
    @Json(name = "id") val id: String,
    @Json(name = "productName") val productName: String,
    @Json(name = "metaUnits") val metaUnits: Int,
    @Json(name = "unitsSoldCurrent") val unitsSoldCurrent: Int,
    @Json(name = "marginUnit") val marginUnit: Double,
    @Json(name = "expectedProfit") val expectedProfit: Double,
    @Json(name = "breakEvenUnits") val breakEvenUnits: Int,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "profitable") val profitable: Boolean,
    @Json(name = "targetAchieved") val targetAchieved: Boolean,
)

