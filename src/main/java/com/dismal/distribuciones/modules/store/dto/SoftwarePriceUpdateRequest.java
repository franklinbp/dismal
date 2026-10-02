package com.dismal.distribuciones.modules.store.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for updating the price of a software product.
 */
public record SoftwarePriceUpdateRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal price
) {
}
