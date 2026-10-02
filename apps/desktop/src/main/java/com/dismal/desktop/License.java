package com.dismal.desktop;

public record License(
        String id,
        String licenseKey,
        String status,
        boolean available,
        int usedActivations,
        int maxActivations,
        double purchasePrice,
        String softwareId,
        String softwareName,
        String ownerId,
        String ownerEmail
) {}
