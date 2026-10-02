package com.dismal.app.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardSummaryDto(
    @Json(name = "todaySalesCount") val todaySalesCount: Long,
    @Json(name = "todaySalesAmount") val todaySalesAmount: Double,
    @Json(name = "todayProfitAmount") val todayProfitAmount: Double,
    @Json(name = "monthSalesCount") val monthSalesCount: Long,
    @Json(name = "monthSalesAmount") val monthSalesAmount: Double,
    @Json(name = "monthProfitAmount") val monthProfitAmount: Double,
    @Json(name = "monthExpectedProfit") val monthExpectedProfit: Double,
    @Json(name = "overdueArCount") val overdueArCount: Long,
    @Json(name = "overdueArBalance") val overdueArBalance: Double,
    @Json(name = "openArCount") val openArCount: Long,
    @Json(name = "openArBalance") val openArBalance: Double,
    @Json(name = "nonProfitableProductsCount") val nonProfitableProductsCount: Long,
    @Json(name = "outboxFailedCount") val outboxFailedCount: Long,
)

