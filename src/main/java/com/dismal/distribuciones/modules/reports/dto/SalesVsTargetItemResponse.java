package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalesVsTargetItemResponse(
        UUID targetId,
        UUID softwareId,
        String softwareName,
        int metaUnits,
        long actualUnits,
        long varianceUnits,
        boolean achieved,
        BigDecimal targetSalePrice,
        LocalDate deadline
) {}
