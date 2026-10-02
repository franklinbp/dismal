package com.dismal.distribuciones.modules.sales.domain;

import com.dismal.distribuciones.modules.storefront.domain.StorefrontCountry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sale_number_counters")
public class SaleNumberCounter {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private StorefrontCountry country;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
