package com.dismal.distribuciones.modules.pricing.dto;

import com.dismal.distribuciones.modules.pricing.domain.PriceListType;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicProductResponse(
        UUID id,
        String name,
        String description,
        String platform,
        String imageUrl,
        String sku,
        String brand,
        boolean physicalProduct,
        int availableQuantity,
        boolean inStock,
        BigDecimal publicPrice,
        BigDecimal effectivePrice,
        BigDecimal finalPrice,
        BigDecimal wholesalePrice,
        PriceListType priceType
) {}
