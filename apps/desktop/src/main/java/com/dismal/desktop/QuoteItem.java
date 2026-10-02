package com.dismal.desktop;

public record QuoteItem(
        String id,
        String softwareId,
        String softwareName,
        int quantity,
        double unitPrice,
        double subtotal
) {}
