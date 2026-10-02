package com.dismal.desktop;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleRow(
        String id,
        String clientId,
        String saleType,
        String status,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SaleItemRow> items
) {}
