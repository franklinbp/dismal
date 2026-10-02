package com.dismal.distribuciones.modules.integrations.woocommerce.repository;

import com.dismal.distribuciones.modules.integrations.woocommerce.domain.WooOrderSync;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WooOrderSyncRepository extends JpaRepository<WooOrderSync, UUID> {
    Optional<WooOrderSync> findByExternalOrderId(String externalOrderId);
}
