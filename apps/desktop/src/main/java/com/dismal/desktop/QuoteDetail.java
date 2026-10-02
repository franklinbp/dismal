package com.dismal.desktop;

import java.util.List;

public record QuoteDetail(
        String id,
        String clientId,
        String clientName,
        String clientEmail,
        String status,
        double total,
        String createdAt,
        String updatedAt,
        String notes,
        String saleId,
        List<QuoteItem> items
) {}
