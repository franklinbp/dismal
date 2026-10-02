package com.dismal.distribuciones.modules.marketing.dto;

import com.dismal.distribuciones.modules.marketing.domain.StrategyActionStatus;
import jakarta.validation.constraints.NotNull;

public record StrategyActionStatusRequest(
        @NotNull StrategyActionStatus status,
        String resultNotes
) {}
