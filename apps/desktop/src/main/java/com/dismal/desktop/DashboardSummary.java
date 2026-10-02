package com.dismal.desktop;

public record DashboardSummary(
        long todaySalesCount,
        double todaySalesAmount,
        long monthSalesCount,
        double monthSalesAmount,
        double monthExpectedProfit,
        long overdueArCount,
        double overdueArBalance,
        long openArCount,
        double openArBalance,
        long nonProfitableProductsCount,
        long outboxFailedCount
) {}
