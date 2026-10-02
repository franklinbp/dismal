package com.dismal.app.domain.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardSummary(
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
) {
    companion object {
        fun empty(): DashboardSummary =
            DashboardSummary(
                todaySalesCount = 0L,
                todaySalesAmount = 0.0,
                todayProfitAmount = 0.0,
                monthSalesCount = 0L,
                monthSalesAmount = 0.0,
                monthProfitAmount = 0.0,
                monthExpectedProfit = 0.0,
                overdueArCount = 0L,
                overdueArBalance = 0.0,
                openArCount = 0L,
                openArBalance = 0.0,
                nonProfitableProductsCount = 0L,
                outboxFailedCount = 0L,
            )
    }
}
