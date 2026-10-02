package com.dismal.distribuciones.modules.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long todaySalesCount,
        BigDecimal todaySalesAmount,
        BigDecimal todayProfitAmount,
        long monthSalesCount,
        BigDecimal monthSalesAmount,
        BigDecimal monthProfitAmount,
        BigDecimal monthExpectedProfit,
        long overdueArCount,
        BigDecimal overdueArBalance,
        long openArCount,
        BigDecimal openArBalance,
        long nonProfitableProductsCount,
        long outboxFailedCount
) {
    public static DashboardSummaryResponse empty() {
        return new DashboardSummaryResponse(
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                0L,
                BigDecimal.ZERO,
                0L,
                0L
        );
    }
}
