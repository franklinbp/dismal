package com.dismal.distribuciones.modules.sales.domain;

import com.dismal.distribuciones.modules.store.domain.Software;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private Software software;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "selected_license_ids", length = 4000)
    private String selectedLicenseIds;

    public List<UUID> getSelectedLicenseIdList() {
        if (selectedLicenseIds == null || selectedLicenseIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(selectedLicenseIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(UUID::fromString)
                .toList();
    }

    public void setSelectedLicenseIdList(List<UUID> licenseIds) {
        if (licenseIds == null || licenseIds.isEmpty()) {
            this.selectedLicenseIds = null;
            return;
        }
        this.selectedLicenseIds = licenseIds.stream()
                .map(UUID::toString)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse(null);
    }
}
