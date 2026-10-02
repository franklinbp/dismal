package com.dismal.distribuciones.modules.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteItemRequest(
        UUID softwareId,
        Integer quantity,
        BigDecimal unitPrice
) {}
