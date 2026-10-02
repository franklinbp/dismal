package com.dismal.distribuciones.modules.planning.domain;

import com.dismal.distribuciones.modules.security.domain.CustomerType;
import com.dismal.distribuciones.modules.store.domain.Software;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sales_targets")
public class SalesTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private Software software;

    @Column(nullable = false)
    private Integer metaUnits;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal salePrice;

    @Column(length = 8)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private CustomerType customerType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal variableCost;

    @Column(precision = 19, scale = 4)
    private BigDecimal fixedCostProduct;

    @Column(nullable = false)
    private Integer unitsSoldCurrent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private SalesTargetStatus status = SalesTargetStatus.ACTIVE;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private Instant closedAt;

    private LocalDate deadline;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
