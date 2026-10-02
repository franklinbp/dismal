package com.dismal.distribuciones.modules.planning.service;

import com.dismal.distribuciones.modules.planning.domain.FixedCostMode;
import com.dismal.distribuciones.modules.planning.domain.SalesTarget;
import com.dismal.distribuciones.modules.expenses.service.ExpenseService;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SalesTargetCalculatorService {

    private final FixedCostMode fixedCostMode;
    private final SalesTargetSettingsService settingsService;
    private final ExpenseService expenseService;

    public SalesTargetCalculatorService(
            SalesTargetSettingsService settingsService,
            ExpenseService expenseService,
            @org.springframework.beans.factory.annotation.Value("${app.sales.fixed-cost-mode:GLOBAL}") String fixedCostMode
    ) {
        this.fixedCostMode = FixedCostMode.valueOf(fixedCostMode);
        this.settingsService = settingsService;
        this.expenseService = expenseService;
    }

    public SalesTargetMetrics calculate(SalesTarget target) {
        BigDecimal salePrice = target.getSalePrice();
        BigDecimal variableCost = target.getVariableCost();
        int metaUnits = target.getMetaUnits();
        int unitsSoldCurrent = target.getUnitsSoldCurrent();

        BigDecimal marginUnit = salePrice.subtract(variableCost);
        BigDecimal targetRevenue = salePrice.multiply(BigDecimal.valueOf(metaUnits));
        BigDecimal variableCostTotal = variableCost.multiply(BigDecimal.valueOf(metaUnits));
        BigDecimal contributionTotal = marginUnit.multiply(BigDecimal.valueOf(metaUnits));
        BigDecimal fixedCostApplied = resolveFixedCost(target);

        Integer breakEvenUnits = null;
        BigDecimal breakEvenRevenue = null;
        if (marginUnit.compareTo(BigDecimal.ZERO) > 0) {
            breakEvenUnits = fixedCostApplied
                    .divide(marginUnit, 0, RoundingMode.CEILING)
                    .intValue();
            breakEvenRevenue = salePrice.multiply(BigDecimal.valueOf(breakEvenUnits));
        }

        BigDecimal expectedProfit = contributionTotal.subtract(fixedCostApplied);
        boolean profitable = marginUnit.compareTo(BigDecimal.ZERO) > 0;
        boolean targetAchieved = unitsSoldCurrent >= metaUnits;

        return new SalesTargetMetrics(
                marginUnit,
                targetRevenue,
                variableCostTotal,
                contributionTotal,
                breakEvenUnits,
                breakEvenRevenue,
                expectedProfit,
                profitable,
                targetAchieved,
                fixedCostApplied
        );
    }

    public BigDecimal resolveFixedCost(SalesTarget target) {
        BigDecimal productFixed = safe(target.getFixedCostProduct());
        if (fixedCostMode == FixedCostMode.PER_PRODUCT) {
            return productFixed;
        }
        // Allow per-product override even when GLOBAL is configured.
        if (productFixed.signum() > 0) {
            return productFixed;
        }
        BigDecimal monthExpenses = safe(expenseService.getCurrentMonthExpensesTotal());
        if (monthExpenses.signum() > 0) {
            return monthExpenses;
        }
        return safe(settingsService.getFixedCostGlobal());
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Getter
    public static class SalesTargetMetrics {
        private final BigDecimal marginUnit;
        private final BigDecimal targetRevenue;
        private final BigDecimal variableCostTotal;
        private final BigDecimal contributionTotal;
        private final Integer breakEvenUnits;
        private final BigDecimal breakEvenRevenue;
        private final BigDecimal expectedProfit;
        private final boolean profitable;
        private final boolean targetAchieved;
        private final BigDecimal fixedCostApplied;

        public SalesTargetMetrics(
                BigDecimal marginUnit,
                BigDecimal targetRevenue,
                BigDecimal variableCostTotal,
                BigDecimal contributionTotal,
                Integer breakEvenUnits,
                BigDecimal breakEvenRevenue,
                BigDecimal expectedProfit,
                boolean profitable,
                boolean targetAchieved,
                BigDecimal fixedCostApplied
        ) {
            this.marginUnit = marginUnit;
            this.targetRevenue = targetRevenue;
            this.variableCostTotal = variableCostTotal;
            this.contributionTotal = contributionTotal;
            this.breakEvenUnits = breakEvenUnits;
            this.breakEvenRevenue = breakEvenRevenue;
            this.expectedProfit = expectedProfit;
            this.profitable = profitable;
            this.targetAchieved = targetAchieved;
            this.fixedCostApplied = fixedCostApplied;
        }
    }
}
