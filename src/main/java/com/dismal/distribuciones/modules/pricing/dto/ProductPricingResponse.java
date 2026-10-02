package com.dismal.distribuciones.modules.pricing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPricingResponse(
        UUID softwareId,
        String softwareName,
        BigDecimal ecFinalPrice,
        BigDecimal ecDistributorPrice,
        BigDecimal peFinalPrice,
        BigDecimal peDistributorPrice
) {}
