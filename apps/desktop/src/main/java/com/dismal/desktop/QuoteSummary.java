package com.dismal.desktop;

public record QuoteSummary(
        String id,
        String clientId,
        String clientName,
        String clientEmail,
        String status,
        double total,
        String createdAt,
        String updatedAt
) {}
