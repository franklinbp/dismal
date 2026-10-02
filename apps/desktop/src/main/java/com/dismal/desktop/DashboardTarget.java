package com.dismal.desktop;

public record DashboardTarget(
        String id,
        String productName,
        int metaUnits,
        int unitsSoldCurrent,
        double marginUnit,
        double expectedProfit,
        Integer breakEvenUnits,
        String deadline,
        boolean profitable,
        boolean targetAchieved
) {}
