package com.dismal.distribuciones.modules.planning.dto;

import java.math.BigDecimal;

public record SalesTargetSummaryResponse(
        int totalTargets,
        int achievedTargets,
        int pendingTargets,
        int totalMetaUnits,
        BigDecimal totalTargetRevenue,
        BigDecimal totalContribution,
        BigDecimal totalFixedCost,
        BigDecimal totalExpectedProfit
) {}
