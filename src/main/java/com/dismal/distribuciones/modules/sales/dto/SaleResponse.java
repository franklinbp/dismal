package com.dismal.distribuciones.modules.sales.dto;

import com.dismal.distribuciones.modules.sales.domain.SaleStatus;
import com.dismal.distribuciones.modules.sales.domain.SaleType;
import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleResponse(
        UUID id,
        String saleNumber,
        UUID clientId,
        SaleType saleType,
        StorefrontCountry country,
        String currency,
        SaleStatus status,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SaleItemResponse> items
) {}
