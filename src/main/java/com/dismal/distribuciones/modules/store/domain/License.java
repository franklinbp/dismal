package com.dismal.distribuciones.modules.store.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.dismal.distribuciones.modules.security.domain.User;
import com.dismal.distribuciones.modules.store.converter.LicenseKeyConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "licenses")
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Convert(converter = LicenseKeyConverter.class)
    @Column(unique = true, nullable = false, updatable = false, length = 1024)
    private String licenseKey;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Builder.Default
    private Integer maxActivations = 1;

    @Builder.Default
    private Integer usedActivations = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private LicenseStatus status = LicenseStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    @JsonIgnore
    private Software software;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // A license is un-owned until sold
    private User owner;

    /**
     * Checks if the license has available activation slots.
     * @return true if used activations are less than max activations, false otherwise.
     */
    public boolean isAvailable() {
        return this.usedActivations < this.maxActivations;
    }
}
