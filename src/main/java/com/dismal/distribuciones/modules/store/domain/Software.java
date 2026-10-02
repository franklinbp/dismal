package com.dismal.distribuciones.modules.store.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "software")
public class Software {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, length = 80)
    private String sku;

    @Column(unique = true, length = 80)
    private String barcode;

    @Column(length = 120)
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private String platform;

    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(precision = 10, scale = 2)
    private BigDecimal heightCm;

    @Column(nullable = false)
    @Builder.Default
    private Boolean physicalProduct = false;

    @OneToMany(mappedBy = "software", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<License> licenses;
}
