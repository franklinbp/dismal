package com.dismal.distribuciones.modules.integrations.woocommerce.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "woo_order_sync",
        uniqueConstraints = @UniqueConstraint(name = "uk_woo_order_sync_external_order_id", columnNames = "external_order_id")
)
public class WooOrderSync {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_order_id", nullable = false, length = 120)
    private String externalOrderId;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(nullable = false, length = 32)
    private String status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
