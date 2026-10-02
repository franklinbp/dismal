package com.dismal.desktop;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesVsTargetItem(
        String targetId,
        String softwareId,
        String softwareName,
        int metaUnits,
        long actualUnits,
        long varianceUnits,
        boolean achieved,
        BigDecimal targetSalePrice,
        LocalDate deadline
) {
    public String getTargetId() {
        return targetId;
    }

    public String getSoftwareId() {
        return softwareId;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public int getMetaUnits() {
        return metaUnits;
    }

    public long getActualUnits() {
        return actualUnits;
    }

    public long getVarianceUnits() {
        return varianceUnits;
    }

    public boolean getAchieved() {
        return achieved;
    }

    public BigDecimal getTargetSalePrice() {
        return targetSalePrice;
    }

    public LocalDate getDeadline() {
        return deadline;
    }
}
