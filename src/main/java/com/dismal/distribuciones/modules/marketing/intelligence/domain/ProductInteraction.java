package com.dismal.distribuciones.modules.marketing.intelligence.domain;

import com.dismal.distribuciones.modules.store.domain.Software;
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
@Table(name = "product_interactions")
public class ProductInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    private Software software;

    private String type; // e.g., "MESSAGE", "CLICK", "VIEW"

    @CreationTimestamp
    private LocalDateTime createdAt;
}
