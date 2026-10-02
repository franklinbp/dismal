package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SalesTarget(
    @Json(name = "id") val id: String,
    @Json(name = "softwareId") val softwareId: String,
    @Json(name = "softwareName") val softwareName: String? = null,
    @Json(name = "metaUnits") val metaUnits: Int,
    @Json(name = "salePrice") val salePrice: Double? = null,
    @Json(name = "variableCost") val variableCost: Double? = null,
    @Json(name = "fixedCostProduct") val fixedCostProduct: Double? = null,
    @Json(name = "unitsSoldCurrent") val unitsSoldCurrent: Int? = null,
    @Json(name = "deadline") val deadline: String,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "marginUnit") val marginUnit: Double? = null,
    @Json(name = "targetRevenue") val targetRevenue: Double? = null,
    @Json(name = "variableCostTotal") val variableCostTotal: Double? = null,
    @Json(name = "contributionTotal") val contributionTotal: Double? = null,
    @Json(name = "breakEvenUnits") val breakEvenUnits: Int? = null,
    @Json(name = "breakEvenRevenue") val breakEvenRevenue: Double? = null,
    @Json(name = "expectedProfit") val expectedProfit: Double? = null,
    @Json(name = "profitable") val profitable: Boolean? = null,
    @Json(name = "targetAchieved") val targetAchieved: Boolean? = null,
    @Json(name = "fixedCostApplied") val fixedCostApplied: Double? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
)
