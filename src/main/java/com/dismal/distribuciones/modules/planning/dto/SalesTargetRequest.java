package com.dismal.distribuciones.modules.planning.dto;

import com.dismal.distribuciones.modules.security.domain.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalesTargetRequest(
        UUID softwareId,
        Integer metaUnits,
        String countryCode,
        CustomerType customerType,
        BigDecimal salePrice,
        BigDecimal variableCost,
        BigDecimal fixedCostProduct,
        Integer unitsSoldCurrent,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate deadline,
        String notes
) {}
