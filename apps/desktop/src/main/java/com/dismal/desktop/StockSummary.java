package com.dismal.desktop;

public record StockSummary(
        String softwareId,
        String softwareName,
        long available,
        long assigned
) {
    public String getSoftwareId() {
        return softwareId;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public long getAvailable() {
        return available;
    }

    public long getAssigned() {
        return assigned;
    }
}
