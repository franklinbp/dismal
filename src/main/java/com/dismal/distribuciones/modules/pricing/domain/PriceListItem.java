package com.dismal.distribuciones.modules.pricing.domain;

import com.dismal.distribuciones.modules.store.domain.Software;
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
@Table(
        name = "price_list_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_price_list_item_list_software", columnNames = {"price_list_id", "software_id"})
)
public class PriceListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private Software software;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
}
