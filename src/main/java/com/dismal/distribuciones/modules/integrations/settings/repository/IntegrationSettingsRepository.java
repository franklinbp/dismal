package com.dismal.distribuciones.modules.integrations.settings.repository;

import com.dismal.distribuciones.modules.integrations.settings.domain.IntegrationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IntegrationSettingsRepository extends JpaRepository<IntegrationSettings, UUID> {
    Optional<IntegrationSettings> findTopByOrderByUpdatedAtDesc();
}
