package com.dismal.distribuciones.modules.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteItemResponse(
        UUID id,
        UUID softwareId,
        String softwareName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
