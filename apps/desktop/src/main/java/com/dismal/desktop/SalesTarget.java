package com.dismal.desktop;

public record SalesTarget(
        String id,
        String softwareId,
        String softwareName,
        int metaUnits,
        double salePrice,
        double variableCost,
        Double fixedCostProduct,
        int unitsSoldCurrent,
        String notes,
        double marginUnit,
        double expectedProfit,
        Integer breakEvenUnits,
        String deadline,
        boolean profitable,
        boolean targetAchieved
) {
    public String getId() {
        return id;
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

    public double getSalePrice() {
        return salePrice;
    }

    public double getVariableCost() {
        return variableCost;
    }

    public Double getFixedCostProduct() {
        return fixedCostProduct;
    }

    public int getUnitsSoldCurrent() {
        return unitsSoldCurrent;
    }

    public String getNotes() {
        return notes;
    }

    public double getMarginUnit() {
        return marginUnit;
    }

    public double getExpectedProfit() {
        return expectedProfit;
    }

    public Integer getBreakEvenUnits() {
        return breakEvenUnits;
    }

    public String getDeadline() {
        return deadline;
    }

    public boolean getProfitable() {
        return profitable;
    }

    public boolean getTargetAchieved() {
        return targetAchieved;
    }
}
