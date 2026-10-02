package com.dismal.desktop;

public record LicenseAssignment(
        String saleId,
        String clientId,
        String clientName,
        String clientEmail,
        String assignedAt
) {}
