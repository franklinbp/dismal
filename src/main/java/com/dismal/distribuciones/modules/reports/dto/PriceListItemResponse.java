package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceListItemResponse(
        UUID id,
        String name,
        String platform,
        BigDecimal price,
        BigDecimal ecFinalPrice,
        BigDecimal ecDistributorPrice,
        BigDecimal peFinalPrice,
        BigDecimal peDistributorPrice,
        long availableStock
) {}
