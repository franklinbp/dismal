package com.dismal.distribuciones.modules.pricing.dto;

import com.dismal.distribuciones.modules.pricing.domain.PriceListType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateProductPriceRequest(
        @NotNull PriceListType type,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price
) {}
