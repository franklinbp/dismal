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
@Table(name = "local_licenses")
@AllArgsConstructor
@NoArgsConstructor
public class LocalLicense {
    @Id
    private String id;
    private String softwareId;
    private String softwareName; // Denormalized for offline view
    private String licenseKey;
    private String status;
    private Integer usedActivations;
    private Integer maxActivations;
    private BigDecimal purchasePrice;
}
