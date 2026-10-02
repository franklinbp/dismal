package com.dismal.distribuciones.modules.integrations.crm.repository;

import com.dismal.distribuciones.modules.integrations.crm.domain.CrmCustomerSync;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrmCustomerSyncRepository extends JpaRepository<CrmCustomerSync, UUID> {
    Optional<CrmCustomerSync> findByUserId(UUID userId);
}
