package com.dismal.desktop;

public record SalesTargetSummary(
        int totalTargets,
        int achievedTargets,
        double totalTargetRevenue,
        double totalExpectedProfit
) {}
