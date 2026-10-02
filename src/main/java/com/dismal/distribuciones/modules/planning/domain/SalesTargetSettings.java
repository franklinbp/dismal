package com.dismal.distribuciones.modules.planning.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_target_settings")
public class SalesTargetSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fixed_cost_global", nullable = false, precision = 19, scale = 2)
    private BigDecimal fixedCostGlobal;
}
