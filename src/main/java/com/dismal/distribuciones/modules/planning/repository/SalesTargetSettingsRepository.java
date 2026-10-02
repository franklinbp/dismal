package com.dismal.distribuciones.modules.planning.repository;

import com.dismal.distribuciones.modules.planning.domain.SalesTargetSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SalesTargetSettingsRepository extends JpaRepository<SalesTargetSettings, Long> {
    Optional<SalesTargetSettings> findTopByOrderByIdAsc();
}
