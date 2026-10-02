package com.dismal.distribuciones.modules.store.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_goals")
public class SalesGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer targetUnits;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal variableCost;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fixedCosts;

    @Column(nullable = false)
    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private Software software;

    // --- GETTERS CALCULADOS (LÓGICA DE NEGOCIO) ---

    public BigDecimal getUnitContributionMargin() {
        if (software == null || software.getPrice() == null || this.variableCost == null) {
            return BigDecimal.ZERO;
        }
        return software.getPrice().subtract(this.variableCost);
    }

    public Integer getBreakEvenUnits() {
        BigDecimal contributionMargin = getUnitContributionMargin();
        if (contributionMargin.compareTo(BigDecimal.ZERO) <= 0) {
            return -1; // Retorna -1 si el punto de equilibrio es inalcanzable
        }
        return this.fixedCosts.divide(contributionMargin, 0, RoundingMode.CEILING).intValue();
    }

    public BigDecimal getBreakEvenSales() {
        if (software == null || software.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        int breakEvenUnits = getBreakEvenUnits();
        if (breakEvenUnits < 0) {
            return null; // Retorna null si es inalcanzable
        }
        return software.getPrice().multiply(new BigDecimal(breakEvenUnits));
    }

    public BigDecimal getExpectedProfit() {
        BigDecimal totalContribution = getUnitContributionMargin().multiply(new BigDecimal(this.targetUnits));
        return totalContribution.subtract(this.fixedCosts);
    }

    public boolean isGoalMet(int currentSales) {
        return currentSales >= this.targetUnits;
    }
}

