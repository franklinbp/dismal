package com.dismal.desktop;

public record ProductLicenseSummary(
        String softwareId,
        String softwareName,
        long availableSerials,
        long totalSerials,
        long usedActivations,
        long maxActivations,
        double avgCost,
        double totalCost,
        double costPerActivation,
        String status
) {}
