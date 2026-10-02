package com.dismal.distribuciones.modules.marketing.dto;

import com.dismal.distribuciones.modules.marketing.domain.StrategyActionChannel;
import com.dismal.distribuciones.modules.marketing.domain.StrategyActionSource;
import com.dismal.distribuciones.modules.marketing.intelligence.domain.MarketingPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record StrategyActionUpdateRequest(
        UUID productId,
        UUID targetId,
        @NotNull StrategyActionSource source,
        @NotNull MarketingPriority priority,
        StrategyActionChannel recommendedChannel,
        @NotBlank String title,
        String description,
        UUID assignedTo,
        LocalDate dueDate,
        String resultNotes
) {}
