package com.dismal.desktop.local.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Entity
@Builder
@Table(name = "local_sales_targets")
@AllArgsConstructor
@NoArgsConstructor
public class LocalSalesTarget {
    @Id
    private String id;
    private String softwareId;
    private String softwareName;
    private Integer metaUnits;
    private Integer unitsSoldCurrent;
    private BigDecimal salePrice;
    private BigDecimal variableCost;
    private BigDecimal fixedCostProduct;
    private String deadline;
    private String notes;
    private boolean profitable;
    private BigDecimal marginUnit;
    private BigDecimal expectedProfit;
}
