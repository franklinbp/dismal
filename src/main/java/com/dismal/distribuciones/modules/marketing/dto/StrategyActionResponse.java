package com.dismal.distribuciones.modules.marketing.dto;

import com.dismal.distribuciones.modules.marketing.domain.StrategyActionChannel;
import com.dismal.distribuciones.modules.marketing.domain.StrategyActionSource;
import com.dismal.distribuciones.modules.marketing.domain.StrategyActionStatus;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StrategyActionResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID targetId,
        StrategyActionSource source,
        MarketingPriority priority,
        StrategyActionStatus status,
        StrategyActionChannel recommendedChannel,
        String title,
        String description,
        UUID assignedTo,
        String assignedToName,
        LocalDate dueDate,
        LocalDateTime completedAt,
        String resultNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
