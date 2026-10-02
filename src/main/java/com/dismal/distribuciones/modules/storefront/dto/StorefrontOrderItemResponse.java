package com.dismal.distribuciones.modules.storefront.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StorefrontOrderItemResponse(
        UUID productId,
        String productName,
        String platform,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
