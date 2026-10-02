package com.dismal.distribuciones.modules.marketing.intelligence.dto;

import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StrategyItemResponse(
        UUID productId,
        String productName,
        MarketingState state,
        MarketingPriority priority,
        Integer metaUnits,
        Integer unitsSoldCurrent,
        BigDecimal targetRevenue,
        BigDecimal expectedProfit,
        double progressPercent,
        double expectedPacePercent,
        boolean behindSchedule,
        LocalDate deadline,
        long daysRemaining,
        String recommendedAction,
        String reason,
        List<String> suggestedActions
) {}
