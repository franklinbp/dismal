package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SalesTargetSummary(
    @Json(name = "totalTargets") val totalTargets: Int,
    @Json(name = "achievedTargets") val achievedTargets: Int,
    @Json(name = "pendingTargets") val pendingTargets: Int,
    @Json(name = "totalMetaUnits") val totalMetaUnits: Int,
    @Json(name = "totalTargetRevenue") val totalTargetRevenue: Double,
    @Json(name = "totalContribution") val totalContribution: Double,
    @Json(name = "totalFixedCost") val totalFixedCost: Double,
    @Json(name = "totalExpectedProfit") val totalExpectedProfit: Double,
)
