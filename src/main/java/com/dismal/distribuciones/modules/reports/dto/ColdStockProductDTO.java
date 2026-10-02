package com.dismal.distribuciones.modules.reports.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing a product that has available stock but no recent sales.
 */
public record ColdStockProductDTO(
        UUID id,
        String name,
        String platform,
        BigDecimal price,
        String imageUrl
) {}
