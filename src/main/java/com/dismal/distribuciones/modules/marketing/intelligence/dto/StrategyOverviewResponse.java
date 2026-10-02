package com.dismal.distribuciones.modules.marketing.intelligence.dto;

import java.math.BigDecimal;
import java.util.List;

public record StrategyOverviewResponse(
        int activeTargets,
        int achievedTargets,
        int targetsBehindSchedule,
        int highPriorityProducts,
        int commercialAlerts,
        int pausedProducts,
        long scheduledCampaigns,
        long failedCampaigns,
        long openStrategyActions,
        BigDecimal activeTargetRevenue,
        BigDecimal activeExpectedProfit,
        List<StrategyItemResponse> priorityItems
) {}
