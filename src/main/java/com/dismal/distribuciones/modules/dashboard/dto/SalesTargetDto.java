package com.dismal.distribuciones.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalesTargetDto(
        UUID id,
        String productName,
        Integer metaUnits,
        Integer unitsSoldCurrent,
        BigDecimal marginUnit,
        BigDecimal expectedProfit,
        Integer breakEvenUnits,
        LocalDate deadline,
        boolean profitable,
        boolean targetAchieved
) {}
