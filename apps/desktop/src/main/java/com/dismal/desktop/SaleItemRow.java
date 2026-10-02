package com.dismal.desktop;

import java.math.BigDecimal;

public record SaleItemRow(
        String id,
        String softwareId,
        String softwareName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}
