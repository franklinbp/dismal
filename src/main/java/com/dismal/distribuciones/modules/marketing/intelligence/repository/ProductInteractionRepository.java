package com.dismal.distribuciones.modules.marketing.intelligence.repository;

import com.dismal.distribuciones.modules.marketing.intelligence.domain.ProductInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ProductInteractionRepository extends JpaRepository<ProductInteraction, UUID> {
    long countBySoftwareIdAndCreatedAtAfter(UUID softwareId, LocalDateTime date);
}
