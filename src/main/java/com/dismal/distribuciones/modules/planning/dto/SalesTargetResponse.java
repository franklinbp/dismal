package com.dismal.distribuciones.modules.planning.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.planning.domain.SalesTargetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SalesTargetResponse(
        UUID id,
        UUID softwareId,
        String softwareName,
        String countryCode,
        CustomerType customerType,
        Integer metaUnits,
        BigDecimal salePrice,
        BigDecimal variableCost,
        BigDecimal fixedCostProduct,
        Integer unitsSoldCurrent,
        SalesTargetStatus status,
        LocalDate periodStart,
        LocalDate periodEnd,
        Instant closedAt,
        LocalDate deadline,
        String notes,
        BigDecimal marginUnit,
        BigDecimal targetRevenue,
        BigDecimal variableCostTotal,
        BigDecimal contributionTotal,
        Integer breakEvenUnits,
        BigDecimal breakEvenRevenue,
        BigDecimal expectedProfit,
        boolean profitable,
        boolean targetAchieved,
        BigDecimal fixedCostApplied,
        Instant createdAt,
        Instant updatedAt
) {}
